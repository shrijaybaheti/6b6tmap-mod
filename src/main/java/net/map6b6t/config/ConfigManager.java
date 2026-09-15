package net.map6b6t.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "6b6tmap.json");
    private static ModConfig config = new ModConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }
                applyDefaults(config);
                save();
            } catch (IOException e) {
                config = new ModConfig();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static ModConfig get() {
        return config;
    }

    private static void applyDefaults(ModConfig loaded) {
        ModConfig defaults = new ModConfig();
        if (loaded.serverUrl == null || loaded.serverUrl.isBlank() || isLocalUrl(loaded.serverUrl)) {
            loaded.serverUrl = defaults.serverUrl;
        } else {
            loaded.serverUrl = ModConfig.sanitizeServerUrl(loaded.serverUrl);
        }
        if (loaded.submitToken == null || loaded.submitToken.isBlank()) {
            loaded.submitToken = defaults.submitToken;
        }
        if (loaded.playerOverride == null) {
            loaded.playerOverride = "";
        }
        if (!loaded.recordWorld && loaded.spawnRadius <= 0) {
            loaded.spawnRadius = ModConfig.DEFAULT_SPAWN_RADIUS;
        }
    }

    private static boolean isLocalUrl(String url) {
        String u = ModConfig.sanitizeServerUrl(url).toLowerCase();
        return u.contains("127.0.0.1") || u.contains("localhost");
    }
}
