package net.map6b6t.scanner;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.registry.Registries;
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
            net.minecraft.block.Block block = Registries.BLOCK.get(rawId);
            if (block == null) return "minecraft:air";
            cached = Registries.BLOCK.getId(block).toString();
            if (BLOCK_NAME_CACHE.size() < CACHE_SIZE) {
                BLOCK_NAME_CACHE.put(rawId, cached);
            }
        }
        return cached;
    }

    public static PrimitiveChunkSnapshot takeSnapshot(WorldChunk chunk) {
        ChunkSection[] originalSections = chunk.getSectionArray();
        int bottomY = chunk.getBottomY();
        if (originalSections == null) {
            return new PrimitiveChunkSnapshot(PrimitiveChunkSnapshot.borrowArray(), 0);
        }
        long[] array = PrimitiveChunkSnapshot.borrowArray();
        int size = 0;
        try {
            for (int sectionIndex = 0; sectionIndex < originalSections.length; sectionIndex++) {
                ChunkSection section = originalSections[sectionIndex];
                if (section == null || section.isEmpty()) {
                    continue;
                }
                int sectionBaseY = bottomY + (sectionIndex << 4);
                for (int ry = 0; ry < 16; ry++) {
                    for (int rz = 0; rz < 16; rz++) {
                        for (int rx = 0; rx < 16; rx++) {
                            BlockState state = section.getBlockState(rx, ry, rz);
                            FluidState fluidState = section.getFluidState(rx, ry, rz);
                            boolean isWater = !fluidState.isEmpty() && fluidState.getFluid() == Fluids.WATER;
                            if (state.isAir() && !isWater) {
                                continue;
                            }
                            int y = sectionBaseY + ry;
                            int rawId = isWater ? -1 : Registries.BLOCK.getRawId(state.getBlock());
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
        } catch (Exception ignored) {
        }
        return new PrimitiveChunkSnapshot(array, size);
    }

    public static void scanAsync(WorldChunk chunk, java.util.function.Consumer<PrimitiveChunkSnapshot> callback) {
        CompletableFuture.runAsync(() -> {
            callback.accept(takeSnapshot(chunk));
        });
    }
}
