package net.map6b6t.gui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class VoidHudLayer12111 {
    private VoidHudLayer12111() {}

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
        for (RenderPipeline.UniformDescription uniform : base.getUniforms()) {
            if (uniform.type() != null) builder.withUniform(uniform.name(), uniform.type());
            else builder.withUniform(uniform.name(), UniformType.TEXEL_BUFFER, uniform.textureFormat());
        }
        RenderPipeline pipeline = builder.build();
        RenderSetup setup = RenderSetup.builder(pipeline).translucent().build();
        for (Method method : RenderLayer.class.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) && p.length == 2 && p[0] == String.class
                    && p[1] == RenderSetup.class && RenderLayer.class.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return (RenderLayer) method.invoke(null, "6b6tmap_void_floor", setup);
            }
        }
        throw new NoSuchMethodException("No compatible RenderSetup RenderLayer factory");
    }
}
