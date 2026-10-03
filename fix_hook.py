
import glob
files = glob.glob("**/SpawnMapMod.java", recursive=True)
for f in files:
    with open(f, "r") as file:
        content = file.read()
    
    # We replace the comment with the actual listener registration!
    if "// Removed CHUNK_LOAD hook" in content:
        content = content.replace("// Removed CHUNK_LOAD hook", "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((client, world, chunk) -> onChunkLoad(world, chunk));")
        with open(f, "w") as file:
            file.write(content)
        print(f"Fixed {f}")

