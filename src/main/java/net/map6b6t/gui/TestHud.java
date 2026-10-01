package net.map6b6t.gui;

import net.minecraft.client.MinecraftClient;

public class TestHud {
    public static void test() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            System.out.println("Found HudRenderCallback");
        } catch (Exception e) {
            System.out.println("No HudRenderCallback: " + e);
        }
        try {
            Class<?> registry = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            System.out.println("Found HudElementRegistry");
            for (java.lang.reflect.Method m : registry.getMethods()) {
                System.out.println("Method: " + m.getName());
            }
        } catch (Exception e) {
            System.out.println("No HudElementRegistry: " + e);
        }
    }
}
