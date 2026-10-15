package net.map6b6t.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class PointerIcon {
    private PointerIcon() {}

    public static void render(GuiGraphicsExtractor graphics, int x, int y, float yaw) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate((float) Math.toRadians(yaw + 180));
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("map6b6t", "textures/gui/minimap_arrow.png"),
                -8, -8, 0f, 0f, 16, 16, 128, 128, 128, 128);
        pose.popMatrix();
    }
}
