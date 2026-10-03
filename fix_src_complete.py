
import os

f = "src/main/java/net/map6b6t/SpawnMapMod.java"
with open(f, "r") as file:
    content = file.read()

# Replace the broken markChunkReceived and onChunkLoad
import re
content = re.sub(r"public void markChunkReceived.*?handleIncomingServerChunk\(chunk\);\s*\}\s*\}", "", content, flags=re.DOTALL)

# Add the correct ones
correct_methods = """
    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.minecraft.util.math.ChunkPos.toLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.world.ClientWorld world, net.minecraft.world.chunk.WorldChunk chunk) {
        if (networkChunks.remove(chunk.getPos().toLong())) {
            handleIncomingServerChunk(chunk);
        }
    }

    public void handleIncomingServerChunk(net.minecraft.world.chunk.WorldChunk chunk) {
        if (chunk == null) {
            return;
        }

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null || !is6b6tServer(client)) {
            return;
        }

        net.map6b6t.config.ModConfig config = net.map6b6t.config.ConfigManager.get();
        if (!config.enabled) {
            return;
        }

        if (client.world != lastWorld) {
            resetSessionCache();
            lastWorld = client.world;
        }

        if (lastSeenHash.size() > LAST_SEEN_MAX) {
            lastSeenHash.clear();
        }

        net.minecraft.util.math.ChunkPos pos = chunk.getPos();
        if (!config.isChunkWithinSpawn(pos.x, pos.z)) {
            return;
        }

        if (net.map6b6t.network.UploadService.get().shouldPauseScanning()) {
            return;
        }

        String playerName = resolvePlayerName(client.player);
        if (config.playerOverride != null && !config.playerOverride.trim().isEmpty()) {
            playerName = config.playerOverride.trim();
        }
        if (playerName == null || playerName.isBlank() || "livemaptest1234".equals(playerName)) {
            net.map6b6t.network.UploadService.get().stats().setLastError("Set your name: /6b6tmap player YourName");
            return;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        String serverVer = resolveServerVersion(client);

        net.map6b6t.network.UploadService.get().submitAsyncScan(
                dimension,
                pos.x,
                pos.z,
                playerName,
                serverVer,
                chunk,
                lastSeenHash::put
        );
    }
"""

content = content.replace("private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());", 
                          "private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());\n" + correct_methods)

# Also remove handleRawPacket completely
content = re.sub(r"public void handleRawPacket.*?if \(rawBytes != null\) \{.*?\}.*?\}", "", content, flags=re.DOTALL)

with open(f, "w") as file:
    file.write(content)
print("Repaired src/main SpawnMapMod")

