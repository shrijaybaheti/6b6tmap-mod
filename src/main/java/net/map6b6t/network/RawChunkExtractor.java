package net.map6b6t.network;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.MinecraftClient;
import java.util.concurrent.CompletableFuture;

public class RawChunkExtractor {
    public static void extractAndUpload(ChunkDataS2CPacket packet) {
        if (UploadService.get().shouldPauseScanning()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        try {
            PacketByteBuf buf = packet.getChunkData().getSectionsDataBuf();
            byte[] rawBytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), rawBytes);
            int chunkX = packet.getChunkX();
            int chunkZ = packet.getChunkZ();
            
            String playerName = client.player.getName().getString();
            if (net.map6b6t.config.ConfigManager.get().playerOverride != null && !net.map6b6t.config.ConfigManager.get().playerOverride.trim().isEmpty()) {
                playerName = net.map6b6t.config.ConfigManager.get().playerOverride.trim();
            }
            if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) return;
            
            String dimension = client.world.getRegistryKey().getValue().toString();
            String serverVer = "1.21.1";
            
            final String fPlayerName = playerName;
            final String fDimension = dimension;
            final String fServerVer = serverVer;
            
            CompletableFuture.runAsync(() -> {
                UploadService.get().submitRawPacketAsync(fDimension, chunkX, chunkZ, fPlayerName, fServerVer, rawBytes, null);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



