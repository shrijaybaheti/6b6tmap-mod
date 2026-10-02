
import os
import glob
import json

mixin_code = """package net.map6b6t.mixin;

import net.map6b6t.SpawnMapMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void beforeChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        SpawnMapMod.getInstance().markChunkReceived(packet.getChunkX(), packet.getChunkZ());
    }
}
"""

mod_code_add = """
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

# Re-add mixin to all versions
version_dirs = ["src/main/java/net/map6b6t"] + glob.glob("versions/*/src/main/java/net/map6b6t")
for vdir in version_dirs:
    mixin_dir = os.path.join(vdir, "mixin")
    os.makedirs(mixin_dir, exist_ok=True)
    with open(os.path.join(mixin_dir, "ClientPlayNetworkHandlerMixin.java"), "w") as f:
        f.write(mixin_code)

# Re-add to mixins.json
mixin_json_files = glob.glob("**/*map6b6t.mixins.json", recursive=True)
for f in mixin_json_files:
    with open(f, "r") as file:
        data = json.load(file)
    if "client" not in data:
        data["client"] = []
    if "ClientPlayNetworkHandlerMixin" not in data["client"]:
        data["client"].append("ClientPlayNetworkHandlerMixin")
        with open(f, "w") as file:
            json.dump(data, file, indent=2)

# Update SpawnMapMod.java
spawn_mod_files = glob.glob("**/SpawnMapMod.java", recursive=True)
for f in spawn_mod_files:
    with open(f, "r") as file:
        content = file.read()
    
    if "markChunkReceived" not in content:
        # replace the generic chunk load with onChunkLoad
        content = content.replace("ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> handleIncomingServerChunk(chunk));",
                                  "ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);")
        
        # insert the new methods after lastWorld declaration
        target = "private ClientWorld lastWorld = null;"
        if target in content:
            content = content.replace(target, target + mod_code_add)
            
        with open(f, "w") as file:
            file.write(content)

