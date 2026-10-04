package net.map6b6t.network;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.map6b6t.scanner.ChunkScanner;
import net.map6b6t.scanner.PrimitiveChunkSnapshot;

public class RawChunkExtractor {
    public static void extractAndUpload(ClientboundLevelChunkWithLightPacket packet) {
        if (UploadService.get().shouldPauseScanning()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        
        try {
            int chunkX = packet.getX();
            int chunkZ = packet.getZ();
            LevelChunk chunk = client.level.getChunk(chunkX, chunkZ);
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
            
            String dimension = net.map6b6t.EnvBridge.getDimension(client.level);
            String serverVer = "1.20.4";
            
            UploadService.get().submitAsyncScan(dimension, chunkX, chunkZ, playerName, serverVer, snapshot, null);
        } catch (Exception ignored) {
        }
    }
}
