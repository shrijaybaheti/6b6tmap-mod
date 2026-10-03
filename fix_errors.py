
import os

# Fix 1: ClientPlayNetworkHandlerMixin.java
mixin_file = "src/main/java/net/map6b6t/mixin/ClientPlayNetworkHandlerMixin.java"
with open(mixin_file, "r") as file:
    content = file.read()
content = content.replace("handleRawPacket(packet.getChunkX(), packet.getChunkZ(), packet);", "markChunkReceived(packet.getChunkX(), packet.getChunkZ());")
with open(mixin_file, "w") as file:
    file.write(content)

# Fix 2 & 3: SpawnMapMod.java
mod_file = "src/main/java/net/map6b6t/SpawnMapMod.java"
with open(mod_file, "r") as file:
    content = file.read()

content = content.replace("net.map6b6t.network.UploadService.get().submitAsyncScan(\n                dimension,\n                pos.x,\n                pos.z,\n                playerName,\n                serverVer,\n                chunk,\n                lastSeenHash::put\n        );", 
"""net.map6b6t.scanner.PrimitiveChunkSnapshot snapshot = net.map6b6t.scanner.ChunkScanner.snapshotAndScan(chunk);
        net.map6b6t.network.UploadService.get().submitAsyncScan(
                dimension,
                pos.x,
                pos.z,
                playerName,
                serverVer,
                snapshot,
                lastSeenHash::put
        );""")

content = content.replace("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> onChunkLoad(world, chunk));", 
"net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);")

with open(mod_file, "w") as file:
    file.write(content)
print("Fixed all compile errors!")

