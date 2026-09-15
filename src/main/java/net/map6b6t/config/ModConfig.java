package net.map6b6t.config;

public class ModConfig {
    public static final int DEFAULT_SPAWN_RADIUS = 5000;

    public boolean enabled = true;
    public String serverUrl = "http://map.6b6t.store/api/chunks/submit";
    public String submitToken = "666819bad6cb7f54d19fae78719b3c4b003699b8cc4b3892";
    public String playerOverride = "";
    public int spawnRadius = DEFAULT_SPAWN_RADIUS;
    public boolean recordWorld = false;
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
        return "spawn " + spawnRadius + " blocks";
    }

    public boolean isWithinSpawn(int blockX, int blockZ) {
        if (recordWorld) {
            return true;
        }
        long r = spawnRadius;
        return (long) blockX * blockX + (long) blockZ * blockZ <= r * r;
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
