package net.map6b6t.gui;

import net.map6b6t.network.UploadService;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinimapOverlay {
    public static final int RADIUS_CHUNKS = 32;
    private static final int VISIBLE_CHUNKS = RADIUS_CHUNKS * 2 + 1;
    private static final int WORLD_SPAN = VISIBLE_CHUNKS * 16;
    private static final int MAP_SIZE = 256;
    private static final int PANEL_WIDTH = 264;
    private static final int PANEL_HEIGHT = 294;
    private static final int MAX_CACHED_CHUNKS = 5000;
    private static final int UPLOADED_TINT = 0xFF20D078;
    private static final int QUEUED_TINT = 0xFFFFA21A;
    private static final int UPLOADING_TINT = 0xFFE43D3D;
    private static final int UNKNOWN_COLOR = 0xFF20252A;
    private static final Map<Long, int[]> SURFACE_COLORS = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };
    private static final Map<Long, UploadService.ChunkUploadState> STATES = new HashMap<>();
    private static final List<Rect> RENDER_RECTS = new ArrayList<>();
    private static String cachedDimension;
    private static int surfaceVersion;
    private static int stateVersion;
    private static int builtSurfaceVersion = -1;
    private static int builtStateVersion = -1;
    private static int builtCenterChunkX = Integer.MIN_VALUE;
    private static int builtCenterChunkZ = Integer.MIN_VALUE;
    private static long lastStateRefresh;

    private MinimapOverlay() {}

    public static void capture(WorldChunk chunk, String dimension) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int[] colors = new int[256];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int y = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
                int color = UNKNOWN_COLOR;
                if (y >= chunk.getBottomY()) {
                    pos.set(chunkX * 16 + x, y, chunkZ * 16 + z);
                    int mapColor = chunk.getBlockState(pos).getMapColor(chunk, pos).color;
                    if (mapColor != 0) color = 0xFF000000 | mapColor;
                }
                colors[z * 16 + x] = color;
            }
        }
        synchronized (SURFACE_COLORS) {
            if (!dimension.equals(cachedDimension)) resetDimension(dimension);
            SURFACE_COLORS.put(ChunkPos.toLong(chunkX, chunkZ), colors);
            surfaceVersion++;
        }
        UploadService.get().markChunkLoaded(dimension, chunkX, chunkZ);
    }

    public static void refreshStates(String dimension, int centerChunkX, int centerChunkZ) {
        long now = System.currentTimeMillis();
        if (now - lastStateRefresh < 250 && dimension.equals(cachedDimension)) return;
        synchronized (SURFACE_COLORS) {
            if (!dimension.equals(cachedDimension)) resetDimension(dimension);
        }
        STATES.clear();
        UploadService uploads = UploadService.get();
        for (int dz = -RADIUS_CHUNKS; dz <= RADIUS_CHUNKS; dz++) {
            for (int dx = -RADIUS_CHUNKS; dx <= RADIUS_CHUNKS; dx++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                STATES.put(ChunkPos.toLong(chunkX, chunkZ), uploads.chunkState(dimension, chunkX, chunkZ));
            }
        }
        lastStateRefresh = now;
        stateVersion++;
    }

    public static UploadService.ChunkUploadState stateAt(int chunkX, int chunkZ) {
        return STATES.getOrDefault(ChunkPos.toLong(chunkX, chunkZ), UploadService.ChunkUploadState.NONE);
    }

    public static List<ChunkPos> cachedChunksWithin(int centerChunkX, int centerChunkZ, int radius) {
        List<ChunkPos> chunks = new ArrayList<>();
        synchronized (SURFACE_COLORS) {
            for (long key : SURFACE_COLORS.keySet()) {
                int chunkX = (int) key;
                int chunkZ = (int) (key >> 32);
                if (Math.abs(chunkX - centerChunkX) <= radius
                        && Math.abs(chunkZ - centerChunkZ) <= radius) {
                    chunks.add(new ChunkPos(chunkX, chunkZ));
                }
            }
        }
        return chunks;
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        ModConfig config = ConfigManager.get();
        int mapSize = Math.max(128, Math.min(512, config.minimapHudSize));
        int panelWidth = mapSize + 8;
        int panelHeight = mapSize + 38;
        int panelX = config.minimapHudX < 0 ? client.getWindow().getScaledWidth() - panelWidth - 8 : config.minimapHudX;
        int panelY = config.minimapHudY;
        panelX = Math.max(0, Math.min(Math.max(0, client.getWindow().getScaledWidth() - panelWidth), panelX));
        panelY = Math.max(0, Math.min(Math.max(0, client.getWindow().getScaledHeight() - panelHeight), panelY));
        renderPreview(context, client, panelX, panelY, mapSize);
    }

    public static void renderPreview(DrawContext context, MinecraftClient client, int panelX, int panelY, int mapSize) {
        if (client.player == null || client.world == null) return;
        mapSize = Math.max(128, Math.min(512, mapSize));
        int centerChunkX = client.player.getChunkPos().x;
        int centerChunkZ = client.player.getChunkPos().z;
        String dimension = client.world.getRegistryKey().getValue().toString();
        refreshStates(dimension, centerChunkX, centerChunkZ);
        synchronized (SURFACE_COLORS) {
            if (!dimension.equals(cachedDimension)) resetDimension(dimension);
        }
        rebuildIfNeeded(centerChunkX, centerChunkZ);

        int mapX = panelX + 4;
        int mapY = panelY + 17;
        context.fill(panelX, panelY, panelX + mapSize + 8, panelY + mapSize + 38, 0xCC080B0E);
        context.drawText(client.textRenderer, Text.literal("6b6t map · 65 chunks"), panelX + 5, panelY + 4, 0xFFFFFFFF, true);
        context.fill(mapX - 1, mapY - 1, mapX + mapSize + 1, mapY + mapSize + 1, 0xFF747A7F);
        for (Rect rect : RENDER_RECTS) {
            int x1 = rect.x * mapSize / MAP_SIZE;
            int y1 = rect.y * mapSize / MAP_SIZE;
            int x2 = (rect.x + rect.width) * mapSize / MAP_SIZE;
            int y2 = (rect.y + rect.height) * mapSize / MAP_SIZE;
            context.fill(mapX + x1, mapY + y1, mapX + Math.max(x1 + 1, x2),
                    mapY + Math.max(y1 + 1, y2), rect.color);
        }

        int startX = (centerChunkX - RADIUS_CHUNKS) * 16;
        int startZ = (centerChunkZ - RADIUS_CHUNKS) * 16;
        int playerX = mapX + (int) ((client.player.getX() - startX) * mapSize / WORLD_SPAN);
        int playerY = mapY + (int) ((client.player.getZ() - startZ) * mapSize / WORLD_SPAN);
        drawPlayerArrow(context, playerX, playerY, client.player.getYaw());

        int legendY = mapY + mapSize + 5;
        boolean compact = mapSize < 192;
        legend(context, client, mapX + 4, legendY, UPLOADED_TINT, compact ? "U" : "Uploaded");
        legend(context, client, mapX + mapSize / 3, legendY, QUEUED_TINT, compact ? "Q" : "Queue");
        legend(context, client, mapX + mapSize * 2 / 3, legendY, UPLOADING_TINT, compact ? "S" : "Sending");
    }

    private static void rebuildIfNeeded(int centerChunkX, int centerChunkZ) {
        if (builtSurfaceVersion == surfaceVersion && builtStateVersion == stateVersion
                && builtCenterChunkX == centerChunkX && builtCenterChunkZ == centerChunkZ) return;

        Map<Long, int[]> surfaces;
        synchronized (SURFACE_COLORS) {
            surfaces = new HashMap<>(SURFACE_COLORS);
        }
        int startX = (centerChunkX - RADIUS_CHUNKS) * 16;
        int startZ = (centerChunkZ - RADIUS_CHUNKS) * 16;
        RENDER_RECTS.clear();
        Map<RunKey, Rect> previousRow = new HashMap<>();

        for (int py = 0; py < MAP_SIZE; py++) {
            int worldZ = startZ + (int) ((py + 0.5) * WORLD_SPAN / MAP_SIZE);
            int runStart = 0;
            int runColor = Integer.MIN_VALUE;
            Map<RunKey, Rect> currentRow = new HashMap<>();
            for (int px = 0; px < MAP_SIZE; px++) {
                int worldX = startX + (int) ((px + 0.5) * WORLD_SPAN / MAP_SIZE);
                int chunkX = Math.floorDiv(worldX, 16);
                int chunkZ = Math.floorDiv(worldZ, 16);
                long key = ChunkPos.toLong(chunkX, chunkZ);
                int[] pixels = surfaces.get(key);
                int color = UNKNOWN_COLOR;
                if (pixels != null) {
                    int localX = Math.floorMod(worldX, 16);
                    int localZ = Math.floorMod(worldZ, 16);
                    color = pixels[localZ * 16 + localX];
                }
                color = switch (STATES.getOrDefault(key, UploadService.ChunkUploadState.NONE)) {
                    case UPLOADED -> blend(color, UPLOADED_TINT, 0.58f);
                    case QUEUED -> blend(color, QUEUED_TINT, 0.62f);
                    case UPLOADING -> blend(color, UPLOADING_TINT, 0.62f);
                    case NONE -> color;
                };

                if (px > 0 && color != runColor) {
                    addRun(py, runStart, px, runColor, previousRow, currentRow);
                    runStart = px;
                }
                runColor = color;
            }
            addRun(py, runStart, MAP_SIZE, runColor, previousRow, currentRow);
            previousRow = currentRow;
        }

        builtSurfaceVersion = surfaceVersion;
        builtStateVersion = stateVersion;
        builtCenterChunkX = centerChunkX;
        builtCenterChunkZ = centerChunkZ;
    }

    private static void addRun(int y, int startX, int endX, int color, Map<RunKey, Rect> previousRow,
                               Map<RunKey, Rect> currentRow) {
        RunKey key = new RunKey(startX, endX - startX, color);
        Rect rect = previousRow.get(key);
        if (rect == null) {
            rect = new Rect(startX, y, endX - startX, 1, color);
            RENDER_RECTS.add(rect);
        } else {
            rect.height++;
        }
        currentRow.put(key, rect);
    }

    private static void legend(DrawContext context, MinecraftClient client, int x, int y, int color, String label) {
        context.fill(x, y + 2, x + 6, y + 8, color);
        context.drawText(client.textRenderer, label, x + 8, y, 0xFFFFFFFF, true);
    }

    private static void drawPlayerArrow(DrawContext context, int x, int y, float yaw) {
        PointerIcon.render(context, x, y, yaw);
    }

    private static int blend(int base, int tint, float amount) {
        int red = (int) (((base >> 16) & 0xFF) * (1 - amount) + ((tint >> 16) & 0xFF) * amount);
        int green = (int) (((base >> 8) & 0xFF) * (1 - amount) + ((tint >> 8) & 0xFF) * amount);
        int blue = (int) ((base & 0xFF) * (1 - amount) + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void resetDimension(String dimension) {
        SURFACE_COLORS.clear();
        STATES.clear();
        RENDER_RECTS.clear();
        cachedDimension = dimension;
        lastStateRefresh = 0;
        surfaceVersion++;
        stateVersion++;
        builtSurfaceVersion = -1;
        builtStateVersion = -1;
    }

    private record RunKey(int x, int width, int color) {}

    public static final class Rect {
        public final int x, y, width, color;
        public int height;
        private Rect(int x, int y, int width, int height, int color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
    }
}
