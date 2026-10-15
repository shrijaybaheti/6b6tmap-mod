package net.map6b6t.network;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import java.util.concurrent.CompletableFuture;

public class RawChunkExtractor {
    public static void extractAndUpload(ClientboundLevelChunkWithLightPacket packet) {
        if (UploadService.get().shouldPauseScanning()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        
        try {
            Object packetData = invokePacket(packet, "getChunkData", "chunkData");
            FriendlyByteBuf buf = (FriendlyByteBuf) packetData.getClass().getMethod("getReadBuffer").invoke(packetData);
            byte[] rawBytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), rawBytes);
            int chunkX = packetX(packet);
            int chunkZ = packetZ(packet);
            int minY = client.level.getMinY();
            int worldHeight = client.level.getHeight();
            
            String playerName = client.player.getName().getString();
            if (playerName == null || playerName.isBlank()) return;
            
            String dimension = net.map6b6t.EnvBridge.getDimension(client.level);
            String serverVer = client.getLaunchedVersion();
            
            final String fPlayerName = playerName;
            final String fDimension = dimension;
            final String fServerVer = serverVer;
            
            CompletableFuture.runAsync(() -> {
                UploadService.get().submitRawPacketAsync(fDimension, chunkX, chunkZ, minY, worldHeight, fPlayerName, fServerVer, rawBytes, null);
            });
        } catch (Exception ignored) {
        }
    }

    public static int packetX(ClientboundLevelChunkWithLightPacket packet) {
        return ((Number) invokePacket(packet, "getX", "x")).intValue();
    }

    public static int packetZ(ClientboundLevelChunkWithLightPacket packet) {
        return ((Number) invokePacket(packet, "getZ", "z")).intValue();
    }

    private static Object invokePacket(Object packet, String legacyName, String modernName) {
        try {
            return packet.getClass().getMethod(legacyName).invoke(packet);
        } catch (ReflectiveOperationException ignored) {
            try { return packet.getClass().getMethod(modernName).invoke(packet); }
            catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        }
    }
}
