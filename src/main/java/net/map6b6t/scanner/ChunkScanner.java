package net.map6b6t.scanner;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Method;
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
            try {
                Class<?> registriesClass = Class.forName("net.minecraft.registry.Registries");
                Object blockRegistry = registriesClass.getField("BLOCK").get(null);
                Method getId = blockRegistry.getClass().getMethod("getId", Object.class);
                cached = getId.invoke(blockRegistry, block).toString();
            } catch (Exception e) {
                try {
                    Class<?> registryClass = Class.forName("net.minecraft.util.registry.Registry");
                    Object blockRegistry = registryClass.getField("BLOCK").get(null);
                    Method getId = blockRegistry.getClass().getMethod("getId", Object.class);
                    cached = getId.invoke(blockRegistry, block).toString();
                } catch (Exception ex) {
                    cached = block.toString();
                }
            }
            BLOCK_NAME_CACHE.put(block, cached);
        }
        return cached;
    }

    public static List<ScannedBlock> scanChunk(WorldChunk chunk) {
        return scanChunkSections(chunk.getSectionArray(), chunk.getBottomY());
    }

    public static List<ScannedBlock> scanChunkSections(ChunkSection[] sections, int bottomY) {
        List<ScannedBlock> list = new ArrayList<>(4096);
        if (sections == null) {
            return list;
        }

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

