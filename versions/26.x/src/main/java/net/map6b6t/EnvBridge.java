package net.map6b6t;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public class EnvBridge {
    public static long asLong(int x, int z) {
        return ChunkPos.toLong(x, z);
    }

    public static int getChunkX(ChunkPos pos) {
        return pos.x();
    }

    public static int getChunkZ(ChunkPos pos) {
        return pos.z();
    }

    public static int getMinBuildHeight(LevelChunk chunk) {
        return chunk.getBottomY();
    }

    public static String getServerVersion(Minecraft client) {
        if (client.getCurrentServer() == null) return null;
        if (client.getCurrentServer().version == null) return null;
        return client.getCurrentServer().version.getString();
    }

    public static String getServerAddress(Minecraft client) {
        if (client.getCurrentServer() == null) return null;
        return client.getCurrentServer().ip;
    }

    public static String getServerBrand(Minecraft client) {
        if (client.getConnection() == null) return null;
        return client.getConnection().serverBrand();
    }
}
