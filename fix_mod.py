
import os
import glob

# Remove all ClientPlayNetworkHandlerMixin.java
mixin_files = glob.glob("**/*ClientPlayNetworkHandlerMixin.java", recursive=True)
for f in mixin_files:
    print(f"Removing {f}")
    os.remove(f)

# Update SpawnMapMod.java
spawn_mod_files = glob.glob("**/SpawnMapMod.java", recursive=True)
for f in spawn_mod_files:
    with open(f, "r") as file:
        content = file.read()
    
    if "ClientChunkEvents.CHUNK_LOAD.register" not in content:
        content = content.replace("ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UploadService.get().shutdown());",
                                  "ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UploadService.get().shutdown());\n        ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> handleIncomingServerChunk(chunk));")
        
        with open(f, "w") as file:
            file.write(content)
        print(f"Updated {f}")

