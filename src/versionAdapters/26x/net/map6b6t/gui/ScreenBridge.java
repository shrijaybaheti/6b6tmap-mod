package net.map6b6t.gui;

import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class ScreenBridge {
    private static Screen nextScreen;
    private static boolean pending;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen screen;
            synchronized (ScreenBridge.class) {
                if (!pending) return;
                screen = nextScreen;
                nextScreen = null;
                pending = false;
            }
            client.setScreenAndShow(screen);
        });
    }

    private ScreenBridge() {}

    public static synchronized void set(Screen screen) {
        nextScreen = screen;
        pending = true;
    }
}
