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
    private final java.util.Set<Long> rawHandledChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.map6b6t.EnvBridge.asLong(x, z));
    }

    public void handleRawChunkPacket(int chunkX, int chunkZ, byte[] raw) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || !is6b6tServer(client)) return;
        ModConfig config = ConfigManager.get();
        if (!config.enabled) return;
        if (!config.isChunkWithinSpawn(chunkX, chunkZ)) return;
        if (UploadService.get().shouldPauseScanning()) {
            markChunkReceived(chunkX, chunkZ);
            return;
        }
        LocalPlayer player = client.player;
        String playerName = resolvePlayerName(player);
        if (playerName == null || playerName.isBlank()) return;
        String dimension = EnvBridge.getDimension(world);
        final String serverVer = resolveServerVersion(client);
        long key = net.map6b6t.EnvBridge.asLong(chunkX, chunkZ);
        rawHandledChunks.add(key);
        UploadService.get().submitRawPacketAsync(dimension, chunkX, chunkZ, playerName, serverVer, raw, (k, v) -> lastSeenHash.put(k, v));
    }

    private void onChunkLoad(net.minecraft.client.multiplayer.ClientLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        long posLong = net.map6b6t.EnvBridge.asLong(net.map6b6t.EnvBridge.getChunkX(chunk.getPos()), net.map6b6t.EnvBridge.getChunkZ(chunk.getPos()));
        if (rawHandledChunks.remove(posLong)) return;
        if (networkChunks.remove(posLong)) {
            handleIncomingServerChunk(chunk);
        }
    }


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

    public void resetSessionCache() {
        lastSeenHash.clear();
    }

    private int setEnabled(FabricClientCommandSource source, boolean enabled) {
        ModConfig config = ConfigManager.get();
        config.enabled = enabled;
        ConfigManager.save();
        source.sendFeedback(createText("6b6t Map " + (enabled ? "enabled" : "disabled")).copy().withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> hudSetting(String target) {
        return net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal(target)
                .executes(context -> setHud(context.getSource(), target, null))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("on").executes(context -> setHud(context.getSource(), target, true)))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("off").executes(context -> setHud(context.getSource(), target, false)));
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
        source.sendFeedback(createText("HUD " + target + " " + (next ? "shown" : "hidden")).copy().withStyle(ChatFormatting.AQUA));
        return 1;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        UploadService.get().setUploadListener((chunkX, chunkZ, contentHash) ->
                lastSeenHash.put(net.map6b6t.EnvBridge.asLong(chunkX, chunkZ), contentHash));
        UploadService.get().start();
        net.map6b6t.gui.HudOverlay.register();
        try {
            Class<?> voidHud;
            try { voidHud = Class.forName("net.map6b6t.gui.VoidHudOverlay263"); }
            catch (ClassNotFoundException ignored) { voidHud = Class.forName("net.map6b6t.gui.VoidHudOverlay262"); }
            voidHud.getMethod("register").invoke(null);
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("net.map6b6t.gui.VoidHudOverlay").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not register the void HUD", exception);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not register the void HUD", exception);
        }

        
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            net.map6b6t.EnvBridge.clearSessionCache();
            UploadService.get().shutdown();
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("6b6tmap")
                .executes(context -> {
                    net.map6b6t.gui.ScreenBridge.set(new net.map6b6t.gui.MapConfigScreen(this::resetSessionCache));
                    return 1;
                })
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("toggle")
                    .executes(context -> setEnabled(context.getSource(), !ConfigManager.get().enabled))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("on").executes(context -> setEnabled(context.getSource(), true)))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("off").executes(context -> setEnabled(context.getSource(), false)))
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
                    .then(hudSetting("status"))
                    .then(hudSetting("map"))
                    .then(hudSetting("void"))
                    .then(hudSetting("nametag"))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("edit").executes(context -> {
                        net.map6b6t.gui.ScreenBridge.set(new net.map6b6t.gui.HudEditorScreen());
                        return 1;
                    }))
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
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("token")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("value", StringArgumentType.string())
                        .executes(context -> {
                            ModConfig config = ConfigManager.get();
                            config.submitToken = StringArgumentType.getString(context, "value");
                            ConfigManager.save();
                            context.getSource().sendFeedback(createText("Upload authentication token updated; it is used to authorize chunk submissions.").copy().withStyle(ChatFormatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("area")
                    .executes(context -> showArea(context.getSource()))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("world").executes(context -> setWorldArea(context.getSource())))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("diameter", IntegerArgumentType.integer(32, ModConfig.MAX_AREA_DIAMETER))
                        .executes(context -> setAreaDiameter(context.getSource(), IntegerArgumentType.getInteger(context, "diameter")))
                    )
                )
            );
        });
    }

    private int showArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        source.sendFeedback(createText("Recording area: " + config.formatBounds()
                + ". Use /6b6tmap area <diameter> or /6b6tmap area world.").copy().withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private int setAreaDiameter(FabricClientCommandSource source, int diameter) {
        ModConfig config = ConfigManager.get();
        if (!config.setAreaDiameter(diameter)) {
            source.sendFeedback(createText("Area diameter must be between 32 and " + ModConfig.MAX_AREA_DIAMETER).copy().withStyle(ChatFormatting.GREEN));
            return 0;
        }
        ConfigManager.save();
        source.sendFeedback(createText("Recording area set to " + config.formatBounds()).copy().withStyle(ChatFormatting.GREEN));
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
    if (world != lastWorld) { resetSessionCache(); lastWorld = world;  }
    if (lastSeenHash.size() > LAST_SEEN_MAX) lastSeenHash.clear();
    ChunkPos pos = chunk.getPos();
    if (!config.isChunkWithinSpawn(net.map6b6t.EnvBridge.getChunkX(pos), net.map6b6t.EnvBridge.getChunkZ(pos))) return;
    if (UploadService.get().shouldPauseScanning()) return;
    LocalPlayer player = client.player;
    String playerName = resolvePlayerName(player);
    if (playerName == null || playerName.isBlank()) return;
    String dimension = EnvBridge.getDimension(world);
    final String serverVer = resolveServerVersion(client);
    final int chunkX = net.map6b6t.EnvBridge.getChunkX(pos);
    final int chunkZ = net.map6b6t.EnvBridge.getChunkZ(pos);
    final String finalPlayerName = playerName;
    net.map6b6t.scanner.ChunkScanner.scanAsync(chunk, snapshot -> {
        UploadService.get().submitAsyncScan(dimension, chunkX, chunkZ, finalPlayerName, serverVer, snapshot, (k, v) -> lastSeenHash.put(k, v));
    });
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










