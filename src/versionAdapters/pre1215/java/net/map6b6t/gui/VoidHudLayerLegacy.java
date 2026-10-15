package net.map6b6t.gui;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

final class VoidHudLayerLegacy {
    private VoidHudLayerLegacy() {}

    static RenderLayer create() throws ReflectiveOperationException {
        Method layerFactory = null;
        Class<?> phasesType = null;
        for (Class<?> type : RenderLayer.class.getDeclaredClasses()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) && p.length == 5 && p[0] == String.class
                        && p[1] == VertexFormat.class && p[2] == VertexFormat.DrawMode.class && p[3] == int.class
                        && RenderLayer.class.isAssignableFrom(method.getReturnType())) {
                    layerFactory = method;
                    phasesType = p[4];
                    break;
                }
            }
            if (layerFactory != null) break;
        }
        if (layerFactory == null) throw new NoSuchMethodException("No compatible multi-phase RenderLayer factory");
        Object phases = buildPhases(phasesType);
        layerFactory.setAccessible(true);
        return (RenderLayer) layerFactory.invoke(null, "6b6tmap_void_floor", VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.QUADS, 256, phases);
    }

    private static Object buildPhases(Class<?> phasesType) throws ReflectiveOperationException {
        VoidHudShaderLegacy.register();
        Class<?> shaderPhase = Class.forName(RenderPhase.class.getName() + "$ShaderProgram");
        var constructor = shaderPhase.getDeclaredConstructor(java.util.function.Supplier.class);
        constructor.setAccessible(true);
        Object program = constructor.newInstance((java.util.function.Supplier<?>) VoidHudShaderLegacy::get);
        Object transparency = phase("translucent_transparency");
        Object depthTest = phase("always");
        Object writeMask = phase("color_mask");
        for (Method factory : phasesType.getDeclaredMethods()) {
            if (!Modifier.isStatic(factory.getModifiers()) || factory.getParameterCount() != 0
                    || factory.getReturnType() == phasesType) continue;
            Class<?> builderType = factory.getReturnType();
            Method build = null;
            for (Method candidate : builderType.getDeclaredMethods()) {
                if (candidate.getParameterCount() == 1 && candidate.getParameterTypes()[0] == boolean.class
                        && phasesType.isAssignableFrom(candidate.getReturnType())) {
                    build = candidate;
                    break;
                }
            }
            if (build == null) continue;
            factory.setAccessible(true);
            Object builder = factory.invoke(null);
            setPhase(builder, program);
            setPhase(builder, transparency);
            setPhase(builder, depthTest);
            setPhase(builder, writeMask);
            build.setAccessible(true);
            return build.invoke(builder, false);
        }
        throw new NoSuchMethodException("No compatible render phases builder");
    }

    private static void setPhase(Object builder, Object phase) throws ReflectiveOperationException {
        for (Method method : builder.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(phase)
                    && method.getReturnType().isAssignableFrom(builder.getClass())) {
                method.setAccessible(true);
                method.invoke(builder, phase);
                return;
            }
        }
        throw new NoSuchMethodException("No phase setter for " + phase);
    }

    private static Object phase(String label) throws ReflectiveOperationException {
        for (Field field : RenderPhase.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object value = field.get(null);
            if (String.valueOf(value).toLowerCase(Locale.ROOT).contains(label)) return value;
        }
        throw new NoSuchFieldException("No matching " + label + " render phase");
    }
}
