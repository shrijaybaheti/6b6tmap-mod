package net.map6b6t.gui;

import net.minecraft.client.gui.screen.Screen;

public final class HudEditorScreens {
    private HudEditorScreens() {}

    public static Screen create() {
        return create(null);
    }

    public static Screen create(Screen parent) {
        for (String name : new String[]{"net.map6b6t.gui.HudEditorScreen12111", "net.map6b6t.gui.HudEditorScreen"}) {
            try { return (Screen) Class.forName(name).getConstructor(Screen.class).newInstance(parent); }
            catch (ReflectiveOperationException ignored) {}
        }
        throw new IllegalStateException("No compatible HUD editor screen is available");
    }
}
