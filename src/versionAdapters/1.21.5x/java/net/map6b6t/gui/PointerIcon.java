package net.map6b6t.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class PointerIcon {
    private static final Method DRAW;
    private static final int LAYER_KIND;
    private static final Function<Identifier, RenderLayer> TEXTURED_LAYER = id -> {
        try { return (RenderLayer) RenderLayer.class.getMethod("getGuiTextured", Identifier.class).invoke(null, id); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    };
    static {
        Method draw = null;
        int layerKind = -1;
        for (Method method : DrawContext.class.getMethods()) {
            if (!method.getName().equals("drawTexture")) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 13 && p[0] == Function.class && p[1] == Identifier.class) { draw = method; layerKind = 1; break; }
            if (p.length == 12 && p[0] == Function.class && p[1] == Identifier.class) { draw = method; layerKind = 2; break; }
            if (p.length == 11 && p[0] == Identifier.class) { draw = method; layerKind = 0; break; }
        }
        if (draw == null) throw new ExceptionInInitializerError("DrawContext texture method not found");
        DRAW = draw;
        LAYER_KIND = layerKind;
    }
    private PointerIcon() {}
    public static void render(DrawContext context, int x, int y, float yaw) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw + 180));
        try {
            Identifier texture = Identifier.of("map6b6t", "textures/gui/minimap_arrow.png");
            if (LAYER_KIND == 1) DRAW.invoke(context, TEXTURED_LAYER, texture, -8, -8, 0f, 0f, 16, 16, 128, 128, 128, 128, 0xFFFFFFFF);
            else if (LAYER_KIND == 2) DRAW.invoke(context, TEXTURED_LAYER, texture, -8, -8, 0f, 0f, 16, 16, 128, 128, 128, 128);
            else DRAW.invoke(context, texture, -8, -8, 16, 16, 0f, 0f, 128, 128, 128, 128);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException("Could not draw minimap pointer", exception); }
        finally { matrices.pop(); }
    }
}
