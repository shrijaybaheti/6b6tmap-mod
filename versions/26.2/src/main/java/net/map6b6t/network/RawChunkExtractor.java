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
            FriendlyByteBuf buf = packet.getChunkData().getReadBuffer();
            byte[] rawBytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), rawBytes);
            int chunkX = packet.getX();
            int chunkZ = packet.getZ();
            
            String playerName = client.player.getName().getString();
            if (net.map6b6t.config.ConfigManager.get().playerOverride != null && !net.map6b6t.config.ConfigManager.get().playerOverride.trim().isEmpty()) {
                playerName = net.map6b6t.config.ConfigManager.get().playerOverride.trim();
            }
            if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) return;
            
            String dimension = net.map6b6t.EnvBridge.getDimension(client.level);
            String serverVer = client.getLaunchedVersion();
            
            final String fPlayerName = playerName;
            final String fDimension = dimension;
            final String fServerVer = serverVer;
            
            CompletableFuture.runAsync(() -> {
                UploadService.get().submitRawPacketAsync(fDimension, chunkX, chunkZ, fPlayerName, fServerVer, rawBytes, null);
            });
        } catch (Exception ignored) {
        }
    }
}
