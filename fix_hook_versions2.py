
import glob
files = glob.glob("versions/*/src/main/java/net/map6b6t/SpawnMapMod.java", recursive=True)
for f in files:
    with open(f, "r") as file:
        content = file.read()
    
    target = "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> onChunkLoad(world, chunk));"
    replacement = "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);"
    
    if target in content:
        content = content.replace(target, replacement)
        with open(f, "w") as file:
            file.write(content)
        print(f"Fixed {f}")

