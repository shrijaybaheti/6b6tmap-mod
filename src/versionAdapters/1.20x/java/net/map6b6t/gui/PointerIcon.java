package net.map6b6t.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class PointerIcon {
    private PointerIcon() {}
    public static void render(DrawContext context, int x, int y, float yaw) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw + 180));
        context.drawTexture(Identifier.of("map6b6t", "textures/gui/minimap_arrow.png"), -8, -8,
                16, 16, 0, 0, 128, 128, 128, 128);
        matrices.pop();
    }
}
