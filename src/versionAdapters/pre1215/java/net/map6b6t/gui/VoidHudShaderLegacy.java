package net.map6b6t.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

final class VoidHudShaderLegacy {
    private static boolean registered;
    private static volatile ShaderProgram program;
    private static Object shaderKey;

    private VoidHudShaderLegacy() {}

    static synchronized void register() throws ReflectiveOperationException {
        if (registered) return;
        registered = true;
        Class<?> callback;
        try {
            callback = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback");
        } catch (ClassNotFoundException noCallbackInThisFabricApi) {
            Class<?> defines = Class.forName("net.minecraft.client.gl.Defines");
            Object emptyDefines = defines.getField("EMPTY").get(null);
            Class<?> key = Class.forName("net.minecraft.client.gl.ShaderProgramKey");
            shaderKey = key.getConstructor(Identifier.class, net.minecraft.client.render.VertexFormat.class, defines)
                    .newInstance(Identifier.of("map6b6t", "core/void_floor_legacy_1214"), VertexFormats.POSITION_COLOR, emptyDefines);
            return;
        }

        Field eventField = callback.getField("EVENT");
        Object event = eventField.get(null);
        Object listener = Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, (proxy, method, args) -> {
            if (method.getName().equals("registerShaders")) {
                Object context = args[0];
                Class<?> contextType = Class.forName(callback.getName() + "$RegistrationContext");
                for (Method register : contextType.getMethods()) {
                    if (register.getName().equals("register") && register.getParameterCount() == 3) {
                        try {
                            register.invoke(context, Identifier.of("map6b6t", "void_floor_legacy"), VertexFormats.POSITION_COLOR,
                                    (Consumer<ShaderProgram>) shader -> program = shader);
                        } catch (java.lang.reflect.InvocationTargetException exception) {
                            throw exception.getCause();
                        } catch (ReflectiveOperationException exception) {
                            throw new java.io.IOException("Could not register the void floor shader", exception);
                        }
                        return null;
                    }
                }
                throw new NoSuchMethodException("Shader registration context method not found");
            }
            return null;
        });
        for (Method register : event.getClass().getMethods()) {
            if (register.getName().equals("register") && register.getParameterCount() == 1
                    && register.getParameterTypes()[0].isAssignableFrom(callback)) {
                register.invoke(event, listener);
                return;
            }
        }
        throw new NoSuchMethodException("Shader callback event registration method not found");
    }

    static ShaderProgram get() {
        ShaderProgram loaded = program;
        if (loaded != null || shaderKey == null) return loaded;
        try {
            Object client = MinecraftClient.getInstance();
            Object loader = client.getClass().getMethod("getShaderLoader").invoke(client);
            loaded = (ShaderProgram) loader.getClass().getMethod("getOrCreateProgram", shaderKey.getClass()).invoke(loader, shaderKey);
            program = loaded;
            return loaded;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not load the void floor shader", exception);
        }
    }
}
