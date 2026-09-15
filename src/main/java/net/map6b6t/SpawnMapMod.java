package net.map6b6t;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnMapMod implements ClientModInitializer {
    private static final int RESCAN_INTERVAL_TICKS = 200;

    private final Map<Long, Integer> lastSeenHash = new ConcurrentHashMap<>();
    private final Map<Long, Integer> lastScanTick = new ConcurrentHashMap<>();
    private final ArrayDeque<ChunkPos> scanQueue = new ArrayDeque<>();
    private final Set<Long> queuedChunkKeys = new HashSet<>();
    private int tickCounter = 0;
    private ClientWorld lastWorld = null;

    private void resetSessionCache() {
        lastSeenHash.clear();
        lastScanTick.clear();
        scanQueue.clear();
        queuedChunkKeys.clear();
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        UploadService.get().setUploadListener((chunkX, chunkZ, contentHash) ->
                lastSeenHash.put(ChunkPos.toLong(chunkX, chunkZ), contentHash));
        UploadService.get().start();
        net.map6b6t.gui.HudOverlay.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UploadService.get().shutdown());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("6b6tmap")
                .then(ClientCommandManager.literal("toggle")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        config.enabled = !config.enabled;
                        ConfigManager.save();
                        context.getSource().sendFeedback(Text.literal("6b6t Map " + (config.enabled ? "enabled" : "disabled")).formatted(Formatting.AQUA));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        ModConfig config = ConfigManager.get();
                        NetworkStats stats = UploadService.get().stats();
                        context.getSource().sendFeedback(Text.literal("Status: " + (config.enabled ? "ACTIVE" : "INACTIVE")
                                + " | " + stats.snapshot()
                                + " | ScanQ: " + scanQueue.size()
                                + " | Tracked: " + lastSeenHash.size()
                                + " | Area: " + config.formatBounds()
                                + " | URL: " + config.serverUrl
                                + (stats.lastError().isBlank() ? "" : " | Err: " + stats.lastError())).formatted(Formatting.GREEN));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("resetcache")
                    .executes(context -> {
                        resetSessionCache();
                        context.getSource().sendFeedback(Text.literal("Cleared scanned chunk session cache.").formatted(Formatting.YELLOW));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("server")
                    .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .executes(context -> {
                            String url = ModConfig.sanitizeServerUrl(StringArgumentType.getString(context, "url"));
                            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                                context.getSource().sendFeedback(Text.literal("URL must start with http:// or https://").formatted(Formatting.RED));
                                return 0;
                            }
                            ModConfig config = ConfigManager.get();
                            config.serverUrl = url;
                            ConfigManager.save();
                            context.getSource().sendFeedback(Text.literal("Server URL updated to: " + url).formatted(Formatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("player")
                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            ModConfig config = ConfigManager.get();
                            config.playerOverride = name;
                            ConfigManager.save();
                            context.getSource().sendFeedback(Text.literal("Player name set to: " + name).formatted(Formatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("token")
                    .then(ClientCommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> {
                            String value = StringArgumentType.getString(context, "value");
                            ModConfig config = ConfigManager.get();
                            config.submitToken = value;
                            ConfigManager.save();
                            context.getSource().sendFeedback(Text.literal("Submit token updated.").formatted(Formatting.AQUA));
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("area")
                    .executes(context -> showArea(context.getSource()))
                    .then(ClientCommandManager.literal("spawn")
                        .executes(context -> setSpawnArea(context.getSource(), ModConfig.DEFAULT_SPAWN_RADIUS))
                        .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(16, 30_000_000))
                            .executes(context -> setSpawnArea(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                        )
                    )
                    .then(ClientCommandManager.literal("world")
                        .executes(context -> setWorldArea(context.getSource()))
                    )
                )
                .then(ClientCommandManager.literal("bounds")
                    .executes(context -> showArea(context.getSource()))
                    .then(ClientCommandManager.literal("spawn")
                        .executes(context -> setSpawnArea(context.getSource(), ModConfig.DEFAULT_SPAWN_RADIUS))
                        .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(16, 30_000_000))
                            .executes(context -> setSpawnArea(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                        )
                    )
                    .then(ClientCommandManager.literal("world")
                        .executes(context -> setWorldArea(context.getSource()))
                    )
                )
            );
        });
    }

    private int showArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        source.sendFeedback(Text.literal("Recording: " + config.formatBounds()
                + ". Default is 5000 from spawn. Use /6b6tmap area world to record anywhere.").formatted(Formatting.AQUA));
        return 1;
    }

    private int setSpawnArea(FabricClientCommandSource source, int radius) {
        ModConfig config = ConfigManager.get();
        config.setSpawnRadius(radius);
        ConfigManager.save();
        source.sendFeedback(Text.literal("Recording limited to " + radius + " blocks from spawn. Chunks outside that are not sent.").formatted(Formatting.GREEN));
        return 1;
    }

    private int setWorldArea(FabricClientCommandSource source) {
        ModConfig config = ConfigManager.get();
        config.setRecordWorld(true);
        ConfigManager.save();
        source.sendFeedback(Text.literal("Recording the whole world. Any loaded chunk you walk near can be sent.").formatted(Formatting.GREEN));
        return 1;
    }

    private void onTick(MinecraftClient client) {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;

        if (world != lastWorld) {
            resetSessionCache();
            lastWorld = world;
        }

        if (world == null || player == null) {
            return;
        }

        ModConfig config = ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        tickCounter++;
        ChunkPos playerPos = player.getChunkPos();
        int radius = 5;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerPos.x + dx;
                int cz = playerPos.z + dz;

                if (!config.isChunkWithinSpawn(cx, cz)) {
                    continue;
                }

                long key = ChunkPos.toLong(cx, cz);
                if (queuedChunkKeys.contains(key)) {
                    continue;
                }
                Integer lastTick = lastScanTick.get(key);
                if (lastTick == null || tickCounter - lastTick >= RESCAN_INTERVAL_TICKS) {
                    queuedChunkKeys.add(key);
                    scanQueue.addLast(new ChunkPos(cx, cz));
                }
            }
        }

        if (UploadService.get().shouldPauseScanning()) {
            return;
        }

        long budgetNanos = 6_000_000L;
        int maxChunksThisTick = 32;
        int processedThisTick = 0;
        long tickStartTime = System.nanoTime();
        String dimension = world.getRegistryKey().getValue().toString();
        String playerName = resolvePlayerName(player);
        if (config.playerOverride != null && !config.playerOverride.trim().isEmpty()) {
            playerName = config.playerOverride.trim();
        }
        if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) {
            UploadService.get().stats().setLastError("Set your name: /6b6tmap player YourName");
            return;
        }
        String serverVer = resolveServerVersion(client);

        while (!scanQueue.isEmpty() && processedThisTick < maxChunksThisTick) {
            if (UploadService.get().shouldPauseScanning()) {
                break;
            }

            ChunkPos target = scanQueue.pollFirst();
            long key = ChunkPos.toLong(target.x, target.z);
            queuedChunkKeys.remove(key);

            WorldChunk chunk = world.getChunk(target.x, target.z);
            if (chunk != null) {
                List<ScannedBlock> scannedBlocks = ChunkScanner.scanChunk(chunk);
                lastScanTick.put(key, tickCounter);

                if (!scannedBlocks.isEmpty()) {
                    int payloadHash = net.map6b6t.network.ChunkSubmission.contentHash(scannedBlocks);
                    Integer lastHash = lastSeenHash.get(key);
                    if (lastHash == null || lastHash != payloadHash) {
                        UploadQueue.OfferResult offer = UploadService.get().submit(
                                dimension, target.x, target.z, playerName, serverVer, scannedBlocks
                        );
                        if (offer == UploadQueue.OfferResult.FULL) {
                            queuedChunkKeys.add(key);
                            scanQueue.addFirst(target);
                            break;
                        }
                    }
                }
            }
            processedThisTick++;
            if (System.nanoTime() - tickStartTime > budgetNanos) {
                break;
            }
        }
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
