package net.map6b6t.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.NetworkStats;
import net.map6b6t.network.UploadService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

public final class HudOverlay {
    private static final int RADIUS = 32;
    private static final int CELLS = RADIUS * 2 + 1;
    private static final int[][] MAP_COLORS = new int[CELLS][CELLS];
    private static long lastMapRefresh;
    private static int cachedChunkX = Integer.MIN_VALUE, cachedChunkZ = Integer.MIN_VALUE;
    private static Object cachedLevel;
    private static String cachedDimension;

    private HudOverlay() {}

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("map6b6t", "hud"), HudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        ModConfig config = ConfigManager.get();
        if (config.showNametagHud) NametagCounts.refreshIfNeeded(client);
        if (config.showHud) drawStatus(graphics, client, config);
        if (config.showMinimap) drawMinimap(graphics, client, config);
    }

    private static void drawStatus(GuiGraphicsExtractor graphics, Minecraft client, ModConfig config) {
        int chunkX = client.player.chunkPosition().x();
        int chunkZ = client.player.chunkPosition().z();
        NetworkStats stats = UploadService.get().stats();
        boolean inArea = config.isChunkWithinSpawn(chunkX, chunkZ);
        String status = !config.enabled ? "6b6t Map: disabled"
                : config.isLocalServerUrl() ? "6b6t Map: set server URL"
                : !inArea ? "6b6t Map: out of bounds"
                : UploadService.get().shouldPauseScanning() ? "6b6t Map: upload backlog"
                : stats.failed() > 0 && stats.uploaded() == 0 ? "6b6t Map: upload failed"
                : "6b6t Map: scanning";
        float scale = Math.max(0.6f, Math.min(2f, config.statusHudScale / 100f));
        String count = "Uploaded: " + stats.uploaded() + "  Queue: " + stats.queueSize()
                + "  Now: " + stats.uploading() + "  Failed: " + stats.failed();
        int textWidth = (int) (Math.max(client.font.width(status), client.font.width(count)) * scale) + 14;
        int x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - textWidth), config.statusHudX));
        int y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - (int) (24 * scale)), config.statusHudY));
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(x, y);
            pose.scale(scale, scale);
            graphics.text(client.font, status, 0, 0, 0xFFFFFFFF, true);
            graphics.text(client.font, count, 0, 11, 0xFFFFFFFF, true);
        } finally {
            pose.popMatrix();
        }
    }

    private static void drawMinimap(GuiGraphicsExtractor graphics, Minecraft client, ModConfig config) {
        int size = Math.max(128, Math.min(512, config.minimapHudSize));
        int left = config.minimapHudX < 0 ? graphics.guiWidth() - size - 8 : config.minimapHudX;
        left = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - size), left));
        int top = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - size - 42), config.minimapHudY));
        renderMinimapPreview(graphics, client, left, top, size);
    }

    public static void renderMinimapPreview(GuiGraphicsExtractor graphics, Minecraft client, int left, int top, int size) {
        if (client.player == null || client.level == null) return;
        size = Math.max(128, Math.min(512, size));
        int chunkX = client.player.chunkPosition().x();
        int chunkZ = client.player.chunkPosition().z();
        String dimension = client.level.dimension().identifier().toString();
        refreshMap(client, dimension, chunkX, chunkZ);
        int cell = Math.max(1, size / CELLS);
        int mapSize = cell * CELLS;
        int mapLeft = left + (size - mapSize) / 2;
        int mapTop = top + 17;

        graphics.fill(left, top, left + size, top + mapSize + 42, 0xCC080B0E);
        graphics.text(client.font, "6b6t map · 65 chunks", left + 5, top + 4, 0xFFFFFFFF, true);
        graphics.fill(mapLeft - 1, mapTop - 1, mapLeft + mapSize + 1, mapTop + mapSize + 1, 0xFF747A7F);
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            int start = -RADIUS;
            int color = MAP_COLORS[dz + RADIUS][0];
            for (int dx = -RADIUS + 1; dx <= RADIUS + 1; dx++) {
                int next = dx <= RADIUS ? MAP_COLORS[dz + RADIUS][dx + RADIUS] : Integer.MIN_VALUE;
                if (next != color) {
                    if (color != 0xFF20252A) {
                        int x1 = mapLeft + (start + RADIUS) * cell;
                        int y1 = mapTop + (dz + RADIUS) * cell;
                        graphics.fill(x1, y1, x1 + (dx - start) * cell, y1 + cell, color);
                    }
                    start = dx;
                    color = next;
                }
            }
        }
        int px = mapLeft + (int) ((RADIUS * 16 + client.player.getX() - chunkX * 16) * mapSize / (CELLS * 16.0));
        int py = mapTop + (int) ((RADIUS * 16 + client.player.getZ() - chunkZ * 16) * mapSize / (CELLS * 16.0));
        drawArrow(graphics, px, py, client.player.getYRot());
        int legendY = mapTop + mapSize + 5;
        boolean compact = size < 192;
        drawLegend(graphics, client, left + 6, legendY, 0xFF20D078, compact ? "U" : "Uploaded");
        drawLegend(graphics, client, left + size / 3, legendY, 0xFFFFA21A, compact ? "Q" : "Queue");
        drawLegend(graphics, client, left + size * 2 / 3, legendY, 0xFFE43D3D, compact ? "S" : "Sending");
    }

    private static void drawLegend(GuiGraphicsExtractor graphics, Minecraft client, int x, int y, int color, String label) {
        graphics.fill(x, y + 2, x + 6, y + 8, color);
        graphics.text(client.font, label, x + 8, y, 0xFFFFFFFF, true);
    }

    private static void refreshMap(Minecraft client, String dimension, int centerX, int centerZ) {
        long now = System.currentTimeMillis();
        if (cachedLevel == client.level && centerX == cachedChunkX && centerZ == cachedChunkZ
                && dimension.equals(cachedDimension) && now - lastMapRefresh < 250) return;
        UploadService uploads = UploadService.get();
        for (int dz = -RADIUS; dz <= RADIUS; dz++) for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            int x = centerX + dx, z = centerZ + dz;
            boolean loaded = client.level.hasChunk(x, z);
            if (loaded) uploads.markChunkLoaded(dimension, x, z);
            int color = colorFor(uploads.chunkState(dimension, x, z));
            if (loaded && color == 0xFF20252A) color = 0xFF566A52;
            MAP_COLORS[dz + RADIUS][dx + RADIUS] = color;
        }
        cachedLevel = client.level;
        cachedChunkX = centerX;
        cachedChunkZ = centerZ;
        cachedDimension = dimension;
        lastMapRefresh = now;
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, float yaw) {
        PointerIcon.render(graphics, x, y, yaw);
    }

    private static int colorFor(UploadService.ChunkUploadState state) {
        return switch (state) {
            case UPLOADED -> 0xFF20D078;
            case QUEUED -> 0xFFFFA21A;
            case UPLOADING -> 0xFFE43D3D;
            case NONE -> 0xFF20252A;
        };
    }
}
