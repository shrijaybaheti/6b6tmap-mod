package net.map6b6t.scanner;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkScanner {
    private static final Map<net.minecraft.block.Block, String> BLOCK_NAME_CACHE = new ConcurrentHashMap<>(512);
    private static final String WATER_ID = "minecraft:water";

    private static String getBlockIdString(net.minecraft.block.Block block) {
        String cached = BLOCK_NAME_CACHE.get(block);
        if (cached == null) {
            cached = Registries.BLOCK.getId(block).toString();
            BLOCK_NAME_CACHE.put(block, cached);
        }
        return cached;
    }

    public static List<ScannedBlock> scanChunk(WorldChunk chunk) {
        List<ScannedBlock> list = new ArrayList<>(4096);
        ChunkSection[] sections = chunk.getSectionArray();
        int bottomY = chunk.getBottomY();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()) {
                continue;
            }

            int sectionBaseY = bottomY + (sectionIndex << 4);

            for (int rx = 0; rx < 16; rx++) {
                for (int rz = 0; rz < 16; rz++) {
                    for (int ry = 0; ry < 16; ry++) {
                        BlockState state = section.getBlockState(rx, ry, rz);
                        FluidState fluidState = section.getFluidState(rx, ry, rz);

                        boolean isWater = !fluidState.isEmpty() && fluidState.getFluid() == Fluids.WATER;

                        if (state.isAir() && !isWater) {
                            continue;
                        }

                        int y = sectionBaseY + ry;
                        String blockName = isWater ? WATER_ID : getBlockIdString(state.getBlock());
                        list.add(new ScannedBlock(rx, y, rz, blockName));
                    }
                }
            }
        }
        return list;
    }
}

