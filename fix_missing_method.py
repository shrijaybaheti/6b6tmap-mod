
import os
import glob

mod_code_add_yarn = """
    private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.minecraft.util.math.ChunkPos.toLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.world.ClientWorld world, net.minecraft.world.chunk.WorldChunk chunk) {
        if (networkChunks.remove(chunk.getPos().toLong())) {
            handleIncomingServerChunk(chunk);
        }
    }
"""

mod_code_add_mojmap = """
    private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.minecraft.world.level.ChunkPos.asLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.multiplayer.ClientLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (networkChunks.remove(chunk.getPos().toLong())) {
            handleIncomingServerChunk(chunk);
        }
    }
"""

spawn_mod_files = glob.glob("**/SpawnMapMod.java", recursive=True)
for f in spawn_mod_files:
    with open(f, "r") as file:
        content = file.read()
    
    if "markChunkReceived" not in content:
        target1 = "private ClientWorld lastWorld = null;"
        target2 = "private ClientLevel lastWorld = null;"
        
        if target1 in content:
            content = content.replace(target1, target1 + mod_code_add_yarn)
            with open(f, "w") as file:
                file.write(content)
            print(f"Updated {f} with Yarn")
        elif target2 in content:
            # Also replace the event handler for Mojmap
            content = content.replace("ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> handleIncomingServerChunk(chunk));",
                                      "ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);")
            content = content.replace(target2, target2 + mod_code_add_mojmap)
            with open(f, "w") as file:
                file.write(content)
            print(f"Updated {f} with Mojmap")

