package net.map6b6t.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.NetworkStats;
import net.map6b6t.network.UploadService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class HudOverlay {
    private HudOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        ModConfig config = ConfigManager.get();
        if (config.showNametagHud) NametagCounts.refreshIfNeeded(client);
        if (config.showHud) renderStatus(context, client, config);
        if (config.showMinimap) MinimapOverlay.render(context, client);
    }

    private static void renderStatus(DrawContext context, MinecraftClient client, ModConfig config) {
        int chunkX = client.player.getChunkPos().x;
        int chunkZ = client.player.getChunkPos().z;
        boolean inSpawn = config.isChunkWithinSpawn(chunkX, chunkZ);
        NetworkStats stats = UploadService.get().stats();
        boolean paused = UploadService.get().shouldPauseScanning();
        String status;
        if (!config.enabled) {
            status = "6b6t Map: DISABLED";
        } else if (config.isLocalServerUrl()) {
            status = "6b6t Map: SET /6b6tmap server";
        } else if (!inSpawn) {
            status = "6b6t Map: OUT OF BOUNDS";
        } else if (paused) {
            status = "6b6t Map: UPLOAD BACKLOG";
        } else if (stats.failed() > 0 && stats.uploaded() == 0) {
            status = "6b6t Map: UPLOAD FAILED";
        } else {
            status = "6b6t Map: SCANNING";
        }
        String count = "Uploaded: " + stats.uploaded() + "  Queue: " + stats.queueSize()
                + "  Now: " + stats.uploading() + "  Failed: " + stats.failed();

        float scale = Math.max(0.6f, Math.min(2f, config.statusHudScale / 100f));
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int textWidth = Math.max(client.textRenderer.getWidth(status), client.textRenderer.getWidth(count));
        int x = Math.max(0, Math.min(screenWidth - (int) (textWidth * scale), config.statusHudX));
        int y = Math.max(0, Math.min(screenHeight - (int) (24 * scale), config.statusHudY));
        Object matrices = context.getMatrices();
        boolean pushed = false;
        try {
            invokeMatrix(matrices, List.of("pushMatrix", "push"));
            pushed = true;
            invokeTranslate(matrices, x, y);
            invokeScale(matrices, scale);
            context.drawText(client.textRenderer, Text.literal(status), 0, 0, 0xFFFFFFFF, true);
            context.drawText(client.textRenderer, Text.literal(count), 0, 11, 0xFFFFFFFF, true);
        } catch (ReflectiveOperationException exception) {
            context.drawText(client.textRenderer, Text.literal(status), x, y, 0xFFFFFFFF, true);
            context.drawText(client.textRenderer, Text.literal(count), x, y + 11, 0xFFFFFFFF, true);
        } finally {
            if (pushed) try { invokeMatrix(matrices, List.of("popMatrix", "pop")); } catch (ReflectiveOperationException ignored) {}
        }
    }

    private static void invokeMatrix(Object matrix, List<String> names) throws ReflectiveOperationException {
        for (String name : names) try { matrix.getClass().getMethod(name).invoke(matrix); return; }
        catch (NoSuchMethodException ignored) {}
        throw new NoSuchMethodException(names.toString());
    }

    private static void invokeTranslate(Object matrix, float x, float y) throws ReflectiveOperationException {
        for (var method : matrix.getClass().getMethods()) {
            if (!method.getName().equals("translate")) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 2 && p[0] == float.class && p[1] == float.class) { method.invoke(matrix, x, y); return; }
            if (p.length == 3 && p[0] == double.class && p[1] == double.class && p[2] == double.class) { method.invoke(matrix, (double)x, (double)y, 0d); return; }
        }
        throw new NoSuchMethodException("translate");
    }

    private static void invokeScale(Object matrix, float scale) throws ReflectiveOperationException {
        for (var method : matrix.getClass().getMethods()) {
            if (!method.getName().equals("scale")) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 2 && p[0] == float.class && p[1] == float.class) { method.invoke(matrix, scale, scale); return; }
            if (p.length == 3 && p[0] == float.class && p[1] == float.class && p[2] == float.class) { method.invoke(matrix, scale, scale, 1f); return; }
        }
        throw new NoSuchMethodException("scale");
    }
}
