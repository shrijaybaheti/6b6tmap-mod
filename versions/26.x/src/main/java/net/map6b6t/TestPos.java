package net.map6b6t;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public class TestPos {
    public static void test(ChunkPos pos, LevelChunk chunk) {
        int x = pos.x;
        int z = pos.z;
        long l = pos.toLong();
        int y = chunk.getMinBuildHeight();
    }
}
