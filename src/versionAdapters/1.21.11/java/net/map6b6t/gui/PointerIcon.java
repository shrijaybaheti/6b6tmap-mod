package net.map6b6t.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

public final class PointerIcon {
    private static final Identifier TEXTURE = Identifier.of("map6b6t", "textures/gui/minimap_arrow.png");
    private PointerIcon() {}
    public static void render(DrawContext context, int x, int y, float yaw) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.rotate((float) Math.toRadians(yaw + 180));
        try { context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, -8, -8,
                0f, 0f, 16, 16, 128, 128, 128, 128); }
        finally { matrices.popMatrix(); }
    }
}
