package net.map6b6t.gui;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.map6b6t.config.ConfigManager;
import net.map6b6t.network.UploadService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.BlendFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class VoidHudOverlay262 {
    private static final Logger LOGGER = LoggerFactory.getLogger("6b6tMap");
    private static RenderType voidLayer;

    private VoidHudOverlay262() {}

    public static void register() {
        try {
            voidLayer = createVoidLayer();
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Could not initialize the through-block void floor render layer", exception);
            return;
        }
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || !ConfigManager.get().showVoidHud) return;
            int radius = Math.max(1, ConfigManager.get().voidHudSize);
            int centerX = client.player.chunkPosition().x();
            int centerZ = client.player.chunkPosition().z();
            String dimension = client.level.dimension().identifier().toString();
            UploadService uploads = UploadService.get();
            var chunks = uploads.loadedChunksWithin(dimension, centerX, centerZ, radius);
            Vec3 camera = context.levelState().cameraRenderState.pos;
            double y = client.level.getMinY() + 0.02 - camera.y;
            double px = Math.floor(client.player.getX() / 16.0) * 16.0 - camera.x;
            double pz = Math.floor(client.player.getZ() / 16.0) * 16.0 - camera.z;
            SubmitNodeCollector collector = context.submitNodeCollector();
            collector.submitCustomGeometry(context.poseStack(), voidLayer, (pose, vertex) -> {
                for (var chunk : chunks) {
                    UploadService.ChunkUploadState state = uploads.chunkState(dimension, chunk.chunkX, chunk.chunkZ);
                    int color = state == UploadService.ChunkUploadState.NONE ? 0xA0607050 : colorFor(state);
                    double x1 = chunk.chunkX * 16.0 - camera.x, x2 = (chunk.chunkX + 1.0) * 16.0 - camera.x;
                    double z1 = chunk.chunkZ * 16.0 - camera.z, z2 = (chunk.chunkZ + 1.0) * 16.0 - camera.z;
                    quad(pose, vertex, x1, y, z1, x2, z2, color);
                }
            });
        });
    }

    private static RenderType createVoidLayer() throws ReflectiveOperationException {
        Field debugFilledSnippet = RenderPipelines.class.getDeclaredField("DEBUG_FILLED_SNIPPET");
        debugFilledSnippet.setAccessible(true);
        RenderPipeline.Builder builder = RenderPipeline.builder((RenderPipeline.Snippet) debugFilledSnippet.get(null))
                .withLocation(Identifier.fromNamespaceAndPath("map6b6t", "void_floor"))
                .withVertexShader(Identifier.fromNamespaceAndPath("map6b6t", "core/void_floor"))
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        RenderPipeline pipeline = builder.build();
        RenderSetup setup = RenderSetup.builder(pipeline).sortOnUpload().createRenderSetup();
        Method factory = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
        factory.setAccessible(true);
        return (RenderType) factory.invoke(null, "6b6tmap_void_floor", setup);
    }

    private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             com.mojang.blaze3d.vertex.VertexConsumer vertex,
                             double x1, double y, double z1, double x2, double z2, int color) {
        point(pose, vertex, x1, y, z1, color);
        point(pose, vertex, x1, y, z2, color);
        point(pose, vertex, x2, y, z2, color);
        point(pose, vertex, x2, y, z1, color);
    }

    private static void point(com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                              com.mojang.blaze3d.vertex.VertexConsumer vertex,
                              double x, double y, double z, int color) {
        vertex.addVertex(pose, (float) x, (float) y, (float) z).setColor(color);
    }

    private static int colorFor(UploadService.ChunkUploadState state) {
        return switch (state) {
            case UPLOADED -> 0xC020D078;
            case QUEUED -> 0xC0FFA21A;
            case UPLOADING -> 0xC0E43D3D;
            case NONE -> 0;
        };
    }
}
