
import os
import glob

mod_code_add_mojmap_fixed = """
    private final java.util.Set<Long> networkChunks = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public void markChunkReceived(int x, int z) {
        networkChunks.add(net.map6b6t.EnvBridge.asLong(x, z));
    }

    private void onChunkLoad(net.minecraft.client.multiplayer.ClientLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        long posLong = net.map6b6t.EnvBridge.asLong(net.map6b6t.EnvBridge.getChunkX(chunk.getPos()), net.map6b6t.EnvBridge.getChunkZ(chunk.getPos()));
        if (networkChunks.remove(posLong)) {
            handleIncomingServerChunk(chunk);
        }
    }
"""

for vdir in ["versions/26.1", "versions/26.2", "versions/26.3", "versions/26.x"]:
    f = os.path.join(vdir, "src/main/java/net/map6b6t/SpawnMapMod.java")
    with open(f, "r") as file:
        content = file.read()
    
    # Remove the bad code
    import re
    content = re.sub(r"private final java\.util\.Set<Long> networkChunks.*?handleIncomingServerChunk\(chunk\);\s*\}\s*\}", mod_code_add_mojmap_fixed.strip(), content, flags=re.DOTALL)
    
    with open(f, "w") as file:
        file.write(content)
    print(f"Updated {f}")

