package net.map6b6t;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.NetworkStats;
import net.map6b6t.network.UploadQueue;
import net.map6b6t.network.UploadService;
import net.map6b6t.scanner.ChunkScanner;
import net.map6b6t.scanner.ScannedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnMapMod implements ClientModInitializer {
    private static SpawnMapMod INSTANCE;
    private final Map<Long, Integer> lastSeenHash = new ConcurrentHashMap<>();
    private static final int LAST_SEEN_MAX = 500_000;
    private ClientWorld lastWorld = null;
    private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final java.util.Set<Long> rawHandledChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.minecraft.util.math.ChunkPos.toLong(x, z));
    }

    public void handleRawChunkPacket(int chunkX, int chunkZ, byte[] raw) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null || !is6b6tServer(client)) return;
        net.map6b6t.config.ModConfig config = net.map6b6t.config.ConfigManager.get();
        if (!config.enabled) return;
        if (!config.isChunkWithinSpawn(chunkX, chunkZ)) return;
        if (net.map6b6t.network.UploadService.get().shouldPauseScanning()) {
            markChunkReceived(chunkX, chunkZ);
            return;
        }
        String playerName = resolvePlayerName(client.player);
        if (playerName == null || playerName.isBlank()) return;
        final String dimension = client.world.getRegistryKey().getValue().toString();
        final String serverVer = resolveServerVersion(client);
        long key = net.minecraft.util.math.ChunkPos.toLong(chunkX, chunkZ);
        rawHandledChunks.add(key);
        net.map6b6t.network.UploadService.get().submitRawPacketAsync(dimension, chunkX, chunkZ, playerName, serverVer, raw, lastSeenHash::put);
    }

    private void onChunkLoad(net.minecraft.client.world.ClientWorld world, net.minecraft.world.chunk.WorldChunk chunk) {
        net.map6b6t.gui.MinimapOverlay.capture(chunk, world.getRegistryKey().getValue().toString());
        long posLong = chunk.getPos().toLong();
        if (rawHandledChunks.remove(posLong)) return;
        if (networkChunks.remove(posLong)) {
            handleIncomingServerChunk(chunk);
        }
    }

    public void handleIncomingServerChunk(net.minecraft.world.chunk.WorldChunk chunk) {
        if (chunk == null) {
            return;
        }

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null || !is6b6tServer(client)) {
            return;
        }

        net.map6b6t.config.ModConfig config = net.map6b6t.config.ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        if (client.world != lastWorld) {
            resetSessionCache();
            lastWorld = client.world;
        }

        if (lastSeenHash.size() > LAST_SEEN_MAX) {
            lastSeenHash.clear();
        }

        final net.minecraft.util.math.ChunkPos pos = chunk.getPos();
        if (!config.isChunkWithinSpawn(pos.x, pos.z)) {
            return;
        }

        if (net.map6b6t.network.UploadService.get().shouldPauseScanning()) {
            return;
        }

        String playerName = resolvePlayerName(client.player);
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        final String dimension = client.world.getRegistryKey().getValue().toString();
        final String serverVer = resolveServerVersion(client);

        final String finalPlayerName = playerName; net.map6b6t.scanner.ChunkScanner.scanAsync(chunk, snapshot -> { net.map6b6t.network.UploadService.get().submitAsyncScan(dimension, pos.x, pos.z, finalPlayerName, serverVer, snapshot, lastSeenHash::put); });
    }


    


        


    


    public SpawnMapMod() {
        INSTANCE = this;
    }

    public static SpawnMapMod getInstance() {
        return INSTANCE;
    }

    public static Text createText(String str) {
        try {
            return (Text) Text.class.getMethod("literal", String.class).invoke(null, str);
        } catch (Exception e) {
            try {
                Class<?> literalTextClass = Class.forName("net.minecraft.text.LiteralText");
                return (Text) literalTextClass.getConstructor(String.class).newInstance(str);
            } catch (Exception ex) {
                return Text.of(str);
            }
        }
    }

    public void resetSessionCache() {
        lastSeenHash.clear();
    }

    private int setEnabled(FabricClientCommandSource source, boolean enabled) {
        ModConfig config = ConfigManager.get();
        config.enabled = enabled;
        ConfigManager.save();
        source.sendFeedback(createText("6b6t Map " + (enabled ? "enabled" : "disabled")).copy().formatted(Formatting.AQUA));
        return 1;
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> hudSetting(String target) {
        return ClientCommandManager.literal(target)
                .executes(context -> setHud(context.getSource(), target, null))
                .then(ClientCommandManager.literal("on").executes(context -> setHud(context.getSource(), target, true)))
                .then(ClientCommandManager.literal("off").executes(context -> setHud(context.getSource(), target, false)));
    }

    private int setHud(FabricClientCommandSource source, String target, Boolean enabled) {
        ModConfig config = ConfigManager.get();
        boolean next;
        switch (target) {
            case "map" -> next = config.showMinimap = enabled == null ? !config.showMinimap : enabled;
            case "void" -> next = config.showVoidHud = enabled == null ? !config.showVoidHud : enabled;
            case "nametag" -> next = config.showNametagHud = enabled == null ? !config.showNametagHud : enabled;
            default -> next = config.showHud = enabled == null ? !config.showHud : enabled;
        }
        ConfigManager.save();
        source.sendFeedback(createText("HUD " + target + " " + (next ? "shown" : "hidden")).copy().formatted(Formatting.AQUA));
        return 1;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        UploadService.get().setUploadListener((chunkX, chunkZ, contentHash) ->
                lastSeenHash.put(ChunkPos.toLong(chunkX, chunkZ), contentHash));
        UploadService.get().start();
        net.map6b6t.gui.HudOverlay.register();
        net.map6b6t.gui.VoidHudOverlay.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UploadService.get().shutdown());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("6b6tmap")
                .executes(context -> {
                    net.map6b6t.gui.ScreenBridge.set(new net.map6b6t.gui.MapConfigScreen(this::resetSessionCache));
                    return 1;
                })
                .then(ClientCommandManager.literal("toggle")
                    .executes(context -> setEnabled(context.getSource(), !ConfigManager.get().enabled))
                    .then(ClientCommandManager.literal("on").executes(context -> setEnabled(context.getSource(), true)))
                    .then(ClientCommandManager.literal("off").executes(context -> setEnabled(context.getSource(), false)))
                )
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        NetworkStats stats = UploadService.get().stats();
                        context.getSource().sendFeedback(createText("Status: " + (config.enabled ? "ACTIVE" : "INACTIVE")
                                + " | " + stats.snapshot()
                                + " | Tracked: " + lastSeenHash.size()
                                + " | Area: " + config.formatBounds()
                                + " | URL: " + config.serverUrl
                                + (stats.lastError().isBlank() ? "" : " | Err: " + stats.lastError())).copy().formatted(Formatting.GREEN));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("resetcache")
                    .executes(context -> {
                        resetSessionCache();
                        context.getSource().sendFeedback(createText("Cleared scanned chunk session cache.").copy().formatted(Formatting.YELLOW));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("hud")
                    .then(hudSetting("status"))
                    .then(hudSetting("map"))
                    .then(hudSetting("void"))
                    .then(hudSetting("nametag"))
                    .then(ClientCommandManager.literal("edit").executes(context -> {
                        net.map6b6t.gui.ScreenBridge.set(net.map6b6t.gui.HudEditorScreens.create());
                        return 1;
                    }))
                )
                .then(ClientCommandManager.literal("gui").executes(context -> {
                    net.map6b6t.gui.ScreenBridge.set(new net.map6b6t.gui.MapConfigScreen(this::resetSessionCache));
                    return 1;
                }))
                .then(ClientCommandManager.literal("server")
                    .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .executes(context -> {
                            String url = ModConfig.sanitizeServerUrl(StringArgumentType.getString(context, "url"));
                            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                                context.getSource().sendFeedback(createText("URL must start with http:// or https://").copy().formatted(Formatting.RED));
                                return 0;
                            }
                            ModConfig config = ConfigManager.get();
                            config.serverUrl = url;
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Server URL updated to: " + url).copy().formatted(Formatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("token")
                    .then(ClientCommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> {
                            ModConfig config = ConfigManager.get();
                            config.submitToken = StringArgumentType.getString(context, "value");
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Upload authentication token updated; it is used to authorize chunk submissions.").copy().formatted(Formatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("area")
                    .executes(context -> showArea(context.getSource()))
                    .then(ClientCommandManager.literal("world").executes(context -> setWorldArea(context.getSource())))
                    .then(ClientCommandManager.argument("diameter", IntegerArgumentType.integer(32, ModConfig.MAX_AREA_DIAMETER))
                        .executes(context -> setAreaDiameter(context.getSource(), IntegerArgumentType.getInteger(context, "diameter")))
                    )
                )
            );
        });
    }

    private int showArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        source.sendFeedback(createText("Recording area: " + config.formatBounds()
                + ". Use /6b6tmap area <diameter> or /6b6tmap area world.").copy().formatted(Formatting.AQUA));
        return 1;
    }

    private int setAreaDiameter(FabricClientCommandSource source, int diameter) {
        ModConfig config = ConfigManager.get();
        if (!config.setAreaDiameter(diameter)) {
            source.sendFeedback(createText("Area diameter must be between 32 and " + ModConfig.MAX_AREA_DIAMETER).copy().formatted(Formatting.GREEN));
            return 0;
        }
        ConfigManager.save();
        source.sendFeedback(createText("Recording area set to " + config.formatBounds()).copy().formatted(Formatting.GREEN));
        return 1;
    }

    private int setWorldArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        config.setRecordWorld(true);
        ConfigManager.save();
        source.sendFeedback(createText("Recording the whole world. Any loaded chunk you walk near can be sent.").copy().formatted(Formatting.GREEN));
        return 1;
    }

    private static final java.util.Set<String> ALLOWED_SERVERS = java.util.Set.of(
            "6b6t.org", "6b6t.net", "6b6t.co", "6b6t.me", "6b6t.cc",
            "7b7t.me", "8b8t.org", "8b8t.xyz", "10b10t.org", 
            "alacity.net", "anarchypvp.pw", "l2x9.org", 
            "simpleanarchy.org", "simpleanarchy.net"
    );

    private static boolean is6b6tServer(MinecraftClient client) {
        if (client.getCurrentServerEntry() == null) return false;
        String addr = client.getCurrentServerEntry().address;
        if (addr == null || addr.isBlank()) return false;
        String host = addr.toLowerCase().trim();
        int colonIdx = host.lastIndexOf(':');
        if (colonIdx > 0) host = host.substring(0, colonIdx);
        for (String allowed : ALLOWED_SERVERS) {
            if (host.equals(allowed) || host.endsWith("." + allowed)) return true;
        }
        return false;
    }

    

    private static String resolveServerVersion(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().version != null) {
            return client.getCurrentServerEntry().version.getString();
        }
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getBrand() != null) {
            return client.getNetworkHandler().getBrand();
        }
        return "1.20.4";
    }

    private static String resolvePlayerName(ClientPlayerEntity player) {
        if (player == null) return "";
        try {
            Object profile = player.getGameProfile();
            if (profile != null) {
                try {
                    java.lang.reflect.Method getName = profile.getClass().getMethod("getName");
                    Object res = getName.invoke(profile);
                    if (res != null) return res.toString();
                } catch (NoSuchMethodException e) {
                    try {
                        java.lang.reflect.Method nameMethod = profile.getClass().getMethod("name");
                        Object res = nameMethod.invoke(profile);
                        if (res != null) return res.toString();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        try {
            return player.getName().getString();
        } catch (Exception ignored) {}

        return "";
    }
}








