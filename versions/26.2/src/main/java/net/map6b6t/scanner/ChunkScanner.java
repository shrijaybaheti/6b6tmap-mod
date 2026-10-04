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
import java.util.concurrent.CompletableFuture;

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

    public static void scanAsync(LevelChunk chunk, java.util.function.Consumer<PrimitiveChunkSnapshot> callback) {
        LevelChunkSection[] originalSections = chunk.getSections();
        int bottomY = -64;
        if (originalSections == null) {
            callback.accept(new PrimitiveChunkSnapshot(PrimitiveChunkSnapshot.borrowArray(), 0));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            long[] array = PrimitiveChunkSnapshot.borrowArray();
            int size = 0;
            
            try {
                for (int sectionIndex = 0; sectionIndex < originalSections.length; sectionIndex++) {
                    LevelChunkSection section = originalSections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    int sectionBaseY = bottomY + (sectionIndex << 4);

                    for (int ry = 0; ry < 16; ry++) {
                        for (int rz = 0; rz < 16; rz++) {
                            for (int rx = 0; rx < 16; rx++) {
                                BlockState state = section.getBlockState(rx, ry, rz);
                                FluidState fluidState = section.getFluidState(rx, ry, rz);

                                boolean isWater = !fluidState.isEmpty() && fluidState.getType() == Fluids.WATER;

                                if (state.isAir() && !isWater) {
                                    continue;
                                }

                                int y = sectionBaseY + ry;
                                int rawId = isWater ? -1 : BuiltInRegistries.BLOCK.getId(state.getBlock());
                                long val = ((long)rx & 0xF) | (((long)y & 0x1FFF) << 4) | (((long)rz & 0xF) << 17) | (((long)rawId & 0xFFFFFFFFL) << 21);
                                
                                if (size >= array.length) {
                                    long[] n = new long[array.length * 2];
                                    System.arraycopy(array, 0, n, 0, array.length);
                                    array = n;
                                }
                                array[size++] = val;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore CME or array out of bounds caused by asynchronous chunk modification
            }
            callback.accept(new PrimitiveChunkSnapshot(array, size));
        });
    }
}




