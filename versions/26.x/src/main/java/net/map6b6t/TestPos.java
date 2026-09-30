package net.map6b6t;

import net.minecraft.world.level.ChunkPos;
import java.lang.reflect.Method;

public class TestPos {
    public static void test() {
        for (Method m : ChunkPos.class.getMethods()) {
            System.out.println("CHUNKPOS_METHOD: " + m.getName());
        }
    }
}
