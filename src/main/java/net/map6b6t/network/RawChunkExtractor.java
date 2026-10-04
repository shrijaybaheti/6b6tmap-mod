package net.map6b6t.network;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.chunk.WorldChunk;
import net.map6b6t.scanner.ChunkScanner;
import net.map6b6t.scanner.PrimitiveChunkSnapshot;

public class RawChunkExtractor {
    public static void extractAndUpload(ChunkDataS2CPacket packet) {
        if (UploadService.get().shouldPauseScanning()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        try {
            int chunkX = packet.getChunkX();
            int chunkZ = packet.getChunkZ();
            WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);
            if (chunk == null) return;
            
            PrimitiveChunkSnapshot snapshot = ChunkScanner.takeSnapshot(chunk);
            if (snapshot.size == 0) {
                snapshot.release();
                return;
            }
            
            String playerName = client.player.getName().getString();
            if (net.map6b6t.config.ConfigManager.get().playerOverride != null && !net.map6b6t.config.ConfigManager.get().playerOverride.trim().isEmpty()) {
                playerName = net.map6b6t.config.ConfigManager.get().playerOverride.trim();
            }
            if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) {
                snapshot.release();
                return;
            }
            
            String dimension = client.world.getRegistryKey().getValue().toString();
            String serverVer = "1.20.4";
            
            UploadService.get().submitAsyncScan(dimension, chunkX, chunkZ, playerName, serverVer, snapshot, null);
        } catch (Exception ignored) {
        }
    }
}
