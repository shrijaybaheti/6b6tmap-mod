
import os

mod_code_add_yarn = """
    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.map6b6t.EnvBridge.asLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.world.ClientWorld world, net.minecraft.world.chunk.WorldChunk chunk) {
        long posLong = net.map6b6t.EnvBridge.asLong(net.map6b6t.EnvBridge.getChunkX(chunk.getPos()), net.map6b6t.EnvBridge.getChunkZ(chunk.getPos()));
        if (networkChunks.remove(posLong)) {
            handleIncomingServerChunk(chunk);
        }
    }
"""

f = "src/main/java/net/map6b6t/SpawnMapMod.java"
with open(f, "r") as file:
    content = file.read()

# I will append markChunkReceived and onChunkLoad after networkChunks definition
if "markChunkReceived" not in content:
    target = "private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());"
    content = content.replace(target, target + "\n" + mod_code_add_yarn)
    
    # And replace the event listener registration in initialize()
    # It might be `ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> handleIncomingServerChunk(chunk));`
    target2 = "ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> handleIncomingServerChunk(chunk));"
    if target2 in content:
        content = content.replace(target2, "ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> onChunkLoad(world, chunk));")
    
    with open(f, "w") as file:
        file.write(content)
    print("Fixed src/main")
else:
    print("Already fixed")

