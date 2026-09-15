package net.map6b6t.gui;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.NetworkStats;
import net.map6b6t.network.UploadService;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HudOverlay {
    public static void register() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            Field eventField = callbackClass.getField("EVENT");
            Object event = eventField.get(null);
            Method registerMethod = event.getClass().getMethod("register", Object.class);

            Object proxy = Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class<?>[] { callbackClass },
                (p, method, args) -> {
                    if (args != null && args.length >= 1 && args[0] != null) {
                        renderHud(args[0]);
                    }
                    return null;
                }
            );

            registerMethod.invoke(event, proxy);
            return;
        } catch (Exception ignored) {
        }

        try {
            Class<?> registry = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            Class<?> hudElement = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement");
            Class<?> identifierClass = Class.forName("net.minecraft.util.Identifier");
            Object id;
            try {
                id = identifierClass.getMethod("of", String.class, String.class).invoke(null, "map6b6t", "status");
            } catch (NoSuchMethodException e) {
                id = identifierClass.getConstructor(String.class, String.class).newInstance("map6b6t", "status");
            }
            Object element = Proxy.newProxyInstance(
                hudElement.getClassLoader(),
                new Class<?>[] { hudElement },
                (p, method, args) -> {
                    if (args != null && args.length >= 1 && args[0] != null) {
                        renderHud(args[0]);
                    }
                    return null;
                }
            );
            try {
                registry.getMethod("addLast", identifierClass, hudElement).invoke(null, id, element);
            } catch (NoSuchMethodException e) {
                registry.getMethod("attachElementAfter", identifierClass, identifierClass, hudElement)
                        .invoke(null, id, id, element);
            }
        } catch (Exception ignored) {
        }
    }

    private static void renderHud(Object renderContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        ModConfig config = ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        int chunkX = client.player.getChunkPos().x;
        int chunkZ = client.player.getChunkPos().z;
        boolean inSpawn = config.isChunkWithinSpawn(chunkX, chunkZ);

        NetworkStats stats = UploadService.get().stats();
        boolean paused = UploadService.get().shouldPauseScanning();
        String status;
        int color;
        if (config.isLocalServerUrl()) {
            status = "6b6t Map: SET /6b6tmap server";
            color = 0xFF5555;
        } else if (!inSpawn) {
            status = "6b6t Map: OUT OF BOUNDS";
            color = 0xFFAA00;
        } else if (paused) {
            status = "6b6t Map: UPLOAD BACKLOG";
            color = 0xFFAA00;
        } else if (stats.failed() > 0 && stats.uploaded() == 0) {
            status = "6b6t Map: UPLOAD FAILED";
            color = 0xFF5555;
        } else {
            status = "6b6t Map: SCANNING";
            color = 0x55FF55;
        }
        String count = "Up " + stats.uploaded() + "  Q " + stats.queueSize() + "  Fail " + stats.failed();
        String extra = stats.lastError() != null && !stats.lastError().isBlank()
                ? stats.lastError()
                : "Area " + config.formatBounds();

        drawText(renderContext, client, status, 8, 8, color);
        drawText(renderContext, client, count, 8, 18, 0xFFFFFF);
        drawText(renderContext, client, extra, 8, 28, 0xAAAAAA);
    }

    private static void drawText(Object context, MinecraftClient client, String text, int x, int y, int color) {
        if (context == null || client.textRenderer == null) {
            return;
        }
        try {
            for (Method m : context.getClass().getMethods()) {
                if (m.getName().startsWith("drawText") || m.getName().equals("method_51433") || m.getName().equals("method_51438")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 5 && params[1] == String.class && params[2] == int.class && params[3] == int.class && params[4] == int.class) {
                        m.invoke(context, client.textRenderer, text, x, y, color);
                        return;
                    }
                    if (params.length == 6 && params[1] == String.class && params[2] == int.class && params[3] == int.class && params[4] == int.class && params[5] == boolean.class) {
                        m.invoke(context, client.textRenderer, text, x, y, color, true);
                        return;
                    }
                    if (params.length == 6 && params[1] != String.class && params[2] == int.class && params[3] == int.class && params[4] == int.class && params[5] == boolean.class) {
                        Object textObj = net.minecraft.text.Text.literal(text);
                        m.invoke(context, client.textRenderer, textObj, x, y, color, true);
                        return;
                    }
                    if (params.length == 5 && params[1] != String.class && params[2] == int.class && params[3] == int.class && params[4] == int.class) {
                        Object textObj = net.minecraft.text.Text.literal(text);
                        m.invoke(context, client.textRenderer, textObj, x, y, color);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Method m : client.textRenderer.getClass().getMethods()) {
                if (m.getName().startsWith("drawWithShadow") || m.getName().equals("method_27521") || m.getName().equals("method_1720")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 5 && params[1] == String.class && (params[2] == float.class || params[2] == int.class)) {
                        m.invoke(client.textRenderer, context, text, (float) x, (float) y, color);
                        return;
                    }
                    if (params.length == 5 && params[1] != String.class && (params[2] == float.class || params[2] == int.class)) {
                        Object textObj = net.minecraft.text.Text.literal(text);
                        m.invoke(client.textRenderer, context, textObj, (float) x, (float) y, color);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
