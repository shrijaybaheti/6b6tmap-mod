package net.map6b6t.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoidHudOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");

    private VoidHudOverlay() {}

    public static void register() {
        for (String adapter : new String[]{
                "net.map6b6t.gui.VoidHudOverlay120",
                "net.map6b6t.gui.VoidHudOverlay121x",
                "net.map6b6t.gui.VoidHudOverlay1215",
                "net.map6b6t.gui.VoidHudOverlay12111"
        }) {
            try {
                Class.forName(adapter).getMethod("register").invoke(null);
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not register the version-specific void floor renderer", exception);
            }
        }
        LOGGER.error("No compatible 6b6t void floor renderer is present in this mod build");
    }
}
