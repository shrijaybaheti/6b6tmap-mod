package net.map6b6t.gui;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.network.UploadService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoidHudOverlay1215 {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");

    private VoidHudOverlay1215() {}

    public static void register() {
        RenderLayer layer;
        try {
            layer = VoidHudLayer1215.create();
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Could not create the no-depth void floor render layer", exception);
            return;
        }
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || !ConfigManager.get().showVoidHud) return;

            int centerX = client.player.getChunkPos().x;
            int centerZ = client.player.getChunkPos().z;
            String dimension = client.world.getRegistryKey().getValue().toString();
            int radius = Math.max(1, ConfigManager.get().voidHudSize);
            UploadService uploads = UploadService.get();
            var chunks = uploads.loadedChunksWithin(dimension, centerX, centerZ, radius);
            if (chunks.isEmpty()) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            Vec3d camera = context.camera().getPos();
            double y = client.world.getBottomY() - camera.y - 0.05;
            VertexConsumer vertices = consumers.getBuffer(layer);
            for (var chunk : chunks) {
                int color = colorFor(uploads.chunkState(dimension, chunk.chunkX, chunk.chunkZ));
                quad(vertices, chunk.chunkX * 16.0 - camera.x, y, chunk.chunkZ * 16.0 - camera.z,
                        (chunk.chunkX + 1.0) * 16.0 - camera.x, (chunk.chunkZ + 1.0) * 16.0 - camera.z, color);
            }
        });
    }

    private static void quad(VertexConsumer vertex, double x1, double y, double z1, double x2, double z2, int color) {
        point(vertex, x1, y, z1, color);
        point(vertex, x1, y, z2, color);
        point(vertex, x2, y, z2, color);
        point(vertex, x2, y, z1, color);
    }

    private static void point(VertexConsumer vertex, double x, double y, double z, int color) {
        vertex.vertex((float) x, (float) y, (float) z).color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF,
                color & 0xFF, (color >>> 24) & 0xFF);
    }

    private static int colorFor(UploadService.ChunkUploadState state) {
        return switch (state) {
            case UPLOADED -> 0xD020D078;
            case QUEUED -> 0xD0FFA21A;
            case UPLOADING -> 0xD0E43D3D;
            case NONE -> 0xB03296E8;
        };
    }
}
