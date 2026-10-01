package net.map6b6t.scanner;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkScanner {
    private static final int CACHE_SIZE = 4096;
    private static final Map<Integer, String> BLOCK_NAME_CACHE = new ConcurrentHashMap<>(CACHE_SIZE);
    public static final String WATER_ID = "minecraft:water";

    public static String getBlockIdStringFromRaw(int rawId) {
        if (rawId == -1) return WATER_ID;
        String cached = BLOCK_NAME_CACHE.get(rawId);
        if (cached == null) {
            net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.byId(rawId);
            if (block == null) return "minecraft:air";
            cached = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (BLOCK_NAME_CACHE.size() < CACHE_SIZE) {
                BLOCK_NAME_CACHE.put(rawId, cached);
            }
        }
        return cached;
    }

    public static PrimitiveChunkSnapshot snapshotAndScan(LevelChunk chunk) {
        return snapshotAndScanSections(chunk.getSections(), net.map6b6t.EnvBridge.getMinBuildHeight(chunk));
    }

    public static PrimitiveChunkSnapshot snapshotAndScanSections(LevelChunkSection[] sections, int bottomY) {
        long[] array = new long[4096];
        int size = 0;
        
        if (sections == null) {
            return new PrimitiveChunkSnapshot(array, 0);
        }

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            int sectionBaseY = bottomY + (sectionIndex << 4);

            for (int rx = 0; rx < 16; rx++) {
                for (int rz = 0; rz < 16; rz++) {
                    for (int ry = 0; ry < 16; ry++) {
                        BlockState state = section.getBlockState(rx, ry, rz);
                        FluidState fluidState = section.getFluidState(rx, ry, rz);

                        boolean isWater = !fluidState.isEmpty() && fluidState.getType() == Fluids.WATER;

                        if (state.isAir() && !isWater) {
                            continue;
                        }

                        int y = sectionBaseY + ry;
                        int rawId = isWater ? -1 : BuiltInRegistries.BLOCK.getId(state.getBlock());
                        long val = ((long)rx & 0xF) | (((long)y & 0x1FFF) << 4) | (((long)rz & 0xF) << 17) | (((long)rawId & 0xFFFFFFFFL) << 21);
                        
                        if (size == array.length) {
                            long[] n = new long[array.length * 2];
                            System.arraycopy(array, 0, n, 0, array.length);
                            array = n;
                        }
                        array[size++] = val;
                    }
                }
            }
        }
        return new PrimitiveChunkSnapshot(array, size);
    }
}


