package net.map6b6t.config;

public class ModConfig {
    public static final int DEFAULT_SPAWN_RADIUS = 25000;
    public static final int MAX_AREA_DIAMETER = 60_000_000;

    public boolean enabled = true;
    public String serverUrl = "http://map.6b6t.store/api/chunks/submit";
    public String submitToken = "666819bad6cb7f54d19fae78719b3c4b003699b8cc4b3892";
    public int spawnRadius = DEFAULT_SPAWN_RADIUS;
    public boolean recordWorld = false;
    public boolean showHud = true;
    public boolean showMinimap = false;
    public boolean showVoidHud = false;
    public boolean showNametagHud = false;
    public int statusHudX = 8;
    public int statusHudY = 8;
    public int statusHudScale = 100;
    public int minimapHudX = -1;
    public int minimapHudY = 8;
    public int minimapHudSize = 256;
    public int voidHudSize = 16;
    public int scanIntervalTicks = 2;

    public boolean isLocalServerUrl() {
        String u = sanitizeServerUrl(serverUrl).toLowerCase();
        return u.isEmpty() || u.contains("127.0.0.1") || u.contains("localhost");
    }

    public static String sanitizeServerUrl(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    public void setSpawnRadius(int radius) {
        recordWorld = false;
        spawnRadius = Math.max(16, radius);
    }

    public boolean setAreaDiameter(long diameter) {
        if (diameter < 32 || diameter > MAX_AREA_DIAMETER) return false;
        setSpawnRadius((int) (diameter / 2));
        return true;
    }

    public void setRecordWorld(boolean enabled) {
        recordWorld = enabled;
        if (!enabled && spawnRadius <= 0) {
            spawnRadius = DEFAULT_SPAWN_RADIUS;
        }
    }

    public String formatBounds() {
        if (recordWorld) {
            return "whole world";
        }
        return "±" + String.format(java.util.Locale.ROOT, "%,d", spawnRadius) + " from (0,0)";
    }

    public boolean isWithinSpawn(int blockX, int blockZ) {
        if (recordWorld) {
            return true;
        }
        return Math.abs((long) blockX) <= spawnRadius && Math.abs((long) blockZ) <= spawnRadius;
    }

    public boolean isChunkWithinSpawn(int chunkX, int chunkZ) {
        if (recordWorld) {
            return true;
        }
        int cMinX = chunkX * 16;
        int cMaxX = cMinX + 15;
        int cMinZ = chunkZ * 16;
        int cMaxZ = cMinZ + 15;
        int nearestX = clamp(0, cMinX, cMaxX);
        int nearestZ = clamp(0, cMinZ, cMaxZ);
        return isWithinSpawn(nearestX, nearestZ);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
