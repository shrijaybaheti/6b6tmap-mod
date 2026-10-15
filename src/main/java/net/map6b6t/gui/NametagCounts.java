package net.map6b6t.gui;

import com.google.gson.JsonParser;
import net.map6b6t.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class NametagCounts {
    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    private static final AtomicLong LAST_REFRESH = new AtomicLong();

    private NametagCounts() {}

    public static void refreshIfNeeded(MinecraftClient client) {
        long now = System.currentTimeMillis();
        long previous = LAST_REFRESH.get();
        if (client.player == null || now - previous < 30_000 || !LAST_REFRESH.compareAndSet(previous, now)) return;
        String names = client.player.networkHandler.getPlayerList().stream().map(PlayerListEntry::getProfile)
                .map(NametagCounts::profileName).filter(name -> name.matches("[A-Za-z0-9_]{1,16}"))
                .distinct().limit(100).reduce((a, b) -> a + "," + b).orElse("");
        if (names.isEmpty()) return;
        Thread worker = new Thread(() -> fetch(names), "6b6tmap-nametag-counts");
        worker.setDaemon(true);
        worker.start();
    }

    public static Integer get(String username) { return COUNTS.get(username.toLowerCase(java.util.Locale.ROOT)); }

    private static String profileName(Object profile) {
        for (String method : new String[]{"name", "getName"}) try {
            return (String) profile.getClass().getMethod(method).invoke(profile);
        } catch (ReflectiveOperationException ignored) {}
        return "";
    }

    private static void fetch(String names) {
        HttpURLConnection connection = null;
        try {
            String configured = ConfigManager.get().serverUrl;
            URI base = URI.create(configured);
            String encoded = URLEncoder.encode(names, StandardCharsets.UTF_8).replace("+", "%20");
            URI endpoint = new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(),
                    "/api/player-upload-counts", "names=" + encoded, null);
            connection = (HttpURLConnection) endpoint.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) return;
            try (var input = connection.getInputStream()) {
                var counts = JsonParser.parseReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonObject("counts");
                COUNTS.clear();
                if (counts != null) for (var entry : counts.entrySet())
                    COUNTS.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), entry.getValue().getAsInt());
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
