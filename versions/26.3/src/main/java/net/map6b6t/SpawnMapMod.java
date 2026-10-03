package net.map6b6t;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnMapMod implements ClientModInitializer {
    private static SpawnMapMod INSTANCE;
    public SpawnMapMod() { INSTANCE = this; }
    public static SpawnMapMod getInstance() { return INSTANCE; }
    private final Map<Long, Integer> lastSeenHash = new ConcurrentHashMap<>();
    private static final int LAST_SEEN_MAX = 500_000;
    private ClientLevel lastWorld = null;
    private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.map6b6t.EnvBridge.asLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.multiplayer.ClientLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        long posLong = net.map6b6t.EnvBridge.asLong(net.map6b6t.EnvBridge.getChunkX(chunk.getPos()), net.map6b6t.EnvBridge.getChunkZ(chunk.getPos()));
        if (networkChunks.remove(posLong)) {
            handleIncomingServerChunk(chunk);
        }
    }

    private String cachedPlayerName = null;
    private String cachedDimension = null;

    public static Component createText(String str) {
        try {
            return (Component) Component.class.getMethod("literal", String.class).invoke(null, str);
        } catch (Exception e) {
            try {
                Class<?> literalComponentClass = Class.forName("net.minecraft.text.LiteralText");
                return (Component) literalComponentClass.getConstructor(String.class).newInstance(str);
            } catch (Exception ex) {
                return Component.literal(str);
            }
        }
    }

    private void resetSessionCache() {
        lastSeenHash.clear();
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        UploadService.get().setUploadListener((chunkX, chunkZ, contentHash) ->
                lastSeenHash.put(net.map6b6t.EnvBridge.asLong(chunkX, chunkZ), contentHash));
        UploadService.get().start();
        net.map6b6t.gui.HudOverlay.register();

        
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            net.map6b6t.EnvBridge.clearSessionCache();
            UploadService.get().shutdown();
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("6b6tmap")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("toggle")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        config.enabled = !config.enabled;
                        ConfigManager.save();
                        context.getSource().sendFeedback(createText("6b6t Map " + (config.enabled ? "enabled" : "disabled")).copy().withStyle(ChatFormatting.AQUA));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("status")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        NetworkStats stats = UploadService.get().stats();
                        context.getSource().sendFeedback(createText("Status: " + (config.enabled ? "ACTIVE" : "INACTIVE")
                                + " | " + stats.snapshot()
                                + " | Tracked: " + lastSeenHash.size()
                                + " | Area: " + config.formatBounds()
                                + " | URL: " + config.serverUrl
                                + (stats.lastError().isBlank() ? "" : " | Err: " + stats.lastError())).copy().withStyle(ChatFormatting.GREEN));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("resetcache")
                    .executes(context -> {
                        resetSessionCache();
                        context.getSource().sendFeedback(createText("Cleared scanned chunk session cache.").copy().withStyle(ChatFormatting.YELLOW));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("hud")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        config.showHud = !config.showHud;
                        ConfigManager.save();
                        context.getSource().sendFeedback(createText("HUD " + (config.showHud ? "shown" : "hidden")).copy().withStyle(ChatFormatting.AQUA));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("server")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> {
                            String url = ModConfig.sanitizeServerUrl(StringArgumentType.getString(context, "url"));
                            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                                context.getSource().sendFeedback(createText("URL must start with http:// or https://").copy().withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            ModConfig config = ConfigManager.get();
                            config.serverUrl = url;
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Server URL updated to: " + url).copy().withStyle(ChatFormatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("player")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            ModConfig config = ConfigManager.get();
                            config.playerOverride = name;
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Player name set to: " + name).copy().withStyle(ChatFormatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("token")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("value", StringArgumentType.string())
                        .executes(context -> {
                            String value = StringArgumentType.getString(context, "value");
                            ModConfig config = ConfigManager.get();
                            config.submitToken = value;
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Submit token updated.").copy().withStyle(ChatFormatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("area")
                    .executes(context -> showArea(context.getSource()))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("spawn")
                        .executes(context -> setSpawnArea(context.getSource(), ModConfig.DEFAULT_SPAWN_RADIUS))
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("radius", IntegerArgumentType.integer(16, 30_000_000))
                            .executes(context -> setSpawnArea(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                        )
                    )
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("world")
                        .executes(context -> setWorldArea(context.getSource()))
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("bounds")
                    .executes(context -> showArea(context.getSource()))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("spawn")
                        .executes(context -> setSpawnArea(context.getSource(), ModConfig.DEFAULT_SPAWN_RADIUS))
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("radius", IntegerArgumentType.integer(16, 30_000_000))
                            .executes(context -> setSpawnArea(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                        )
                    )
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("world")
                        .executes(context -> setWorldArea(context.getSource()))
                    )
                )
            );
        });
    }

    private int showArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        source.sendFeedback(createText("Recording: " + config.formatBounds()
                + ". Default is 5000 from spawn. Use /6b6tmap area world to record anywhere.").copy().withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private int setSpawnArea(FabricClientCommandSource source, int radius) {
        ModConfig config = ConfigManager.get();
        config.setSpawnRadius(radius);
        ConfigManager.save();
        source.sendFeedback(createText("Recording limited to " + radius + " blocks from spawn. Chunks outside that are not sent.").copy().withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private int setWorldArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        config.setRecordWorld(true);
        ConfigManager.save();
        source.sendFeedback(createText("Recording the whole world. Any loaded chunk you walk near can be sent.").copy().withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static final java.util.Set<String> ALLOWED_SERVERS = java.util.Set.of(
            "6b6t.org", "6b6t.net", "6b6t.co", "6b6t.me", "6b6t.cc",
            "7b7t.me", "8b8t.org", "8b8t.xyz", "10b10t.org", 
            "alacity.net", "anarchypvp.pw", "l2x9.org", 
            "simpleanarchy.org", "simpleanarchy.net"
    );

    private static boolean is6b6tServer(Minecraft client) {
        if (net.map6b6t.EnvBridge.getServerAddress(client) == null) return false;
        String addr = net.map6b6t.EnvBridge.getServerAddress(client);
        if (addr == null || addr.isBlank()) return false;
        String host = addr.toLowerCase().trim();
        int colonIdx = host.lastIndexOf(':');
        if (colonIdx > 0) host = host.substring(0, colonIdx);
        for (String allowed : ALLOWED_SERVERS) {
            if (host.equals(allowed) || host.endsWith("." + allowed)) return true;
        }
        return false;
    }

    public void handleIncomingServerChunk(LevelChunk chunk) {
    if (chunk == null) return;
    Minecraft client = Minecraft.getInstance();
    ClientLevel world = client.level;
    if (world == null || !is6b6tServer(client)) return;
    ModConfig config = ConfigManager.get();
    if (!config.enabled) return;
    if (world != lastWorld) { resetSessionCache(); lastWorld = world; cachedDimension = null; cachedPlayerName = null; }
    if (lastSeenHash.size() > LAST_SEEN_MAX) lastSeenHash.clear();
    ChunkPos pos = chunk.getPos();
    if (!config.isChunkWithinSpawn(net.map6b6t.EnvBridge.getChunkX(pos), net.map6b6t.EnvBridge.getChunkZ(pos))) return;
    if (UploadService.get().shouldPauseScanning()) return;
    LocalPlayer player = client.player;
    String playerName = resolvePlayerName(player);
    if (config.playerOverride != null && !config.playerOverride.trim().isEmpty()) playerName = config.playerOverride.trim();
    if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) return;
    String dimension = EnvBridge.getDimension(world);
    String serverVer = resolveServerVersion(client);
    UploadService.get().submitAsyncScan(dimension, chunk, net.map6b6t.EnvBridge.getChunkX(pos), net.map6b6t.EnvBridge.getChunkZ(pos), playerName, serverVer, (k, v) -> lastSeenHash.put(k, v));
}

    private static String resolveServerVersion(Minecraft client) {
        if (net.map6b6t.EnvBridge.getServerVersion(client) != null) {
            return net.map6b6t.EnvBridge.getServerVersion(client);
        }
        if (net.map6b6t.EnvBridge.getServerBrand(client) != null) {
            return net.map6b6t.EnvBridge.getServerBrand(client);
        }
        return "1.20.4";
    }

    private static String resolvePlayerName(LocalPlayer player) {
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




