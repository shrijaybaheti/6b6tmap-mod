package net.map6b6t.gui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class VoidHudLayer1215 {
    private VoidHudLayer1215() {}

    static RenderLayer create() throws ReflectiveOperationException {
        RenderPipeline base = RenderPipelines.DEBUG_QUADS;
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation("pipeline/6b6tmap_void_floor")
                .withVertexShader(Identifier.of("map6b6t", "core/void_floor"))
                .withFragmentShader(base.getFragmentShader())
                .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode())
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .withCull(false);
        for (RenderPipeline.UniformDescription uniform : base.getUniforms())
            builder.withUniform(uniform.name(), uniform.type());
        RenderPipeline pipeline = builder.build();

        Object defaultLayer = RenderLayer.getDebugQuads();
        Method factory = null;
        Class<?> phasesType = null;
        for (Method method : RenderLayer.class.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) && p.length == 4 && p[0] == String.class
                    && p[1] == int.class && p[2] == RenderPipeline.class
                    && RenderLayer.class.isAssignableFrom(method.getReturnType())) {
                factory = method;
                phasesType = p[3];
                break;
            }
        }
        if (factory == null) throw new NoSuchMethodException("No compatible render-pipeline RenderLayer factory");
        Object phases = null;
        for (Class<?> type = defaultLayer.getClass(); type != null && phases == null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!phasesType.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                phases = field.get(defaultLayer);
                break;
            }
        }
        if (phases == null) throw new NoSuchFieldException("Debug quad render phases");
        factory.setAccessible(true);
        return (RenderLayer) factory.invoke(null, "6b6tmap_void_floor", 256, pipeline, phases);
    }
}
