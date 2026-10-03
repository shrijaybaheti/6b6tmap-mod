
import os

# Fix ChunkSubmission.java
f = "src/main/java/net/map6b6t/network/ChunkSubmission.java"
with open(f, "r") as file:
    content = file.read()

content = content.replace("this(dimension, chunkX, chunkZ, playerName, serverVersion, rawBytes, contentHash, null);", "this(dimension, chunkX, chunkZ, playerName, serverVersion, blocks, contentHash, null);")
content = content.replace("public static int contentHash(byte[] rawBytes) {\n        return blocks != null ? blocks.hashCode() : 0;\n    }", "public static int contentHash(Object blocks) {\n        return blocks != null ? blocks.hashCode() : 0;\n    }")

with open(f, "w") as file:
    file.write(content)

# Fix UploadService.java (all versions and src/main)
import glob
files = glob.glob("**/UploadService.java", recursive=True)
for uf in files:
    with open(uf, "r") as file:
        ucontent = file.read()
    ucontent = ucontent.replace("stats.incrementTracked();", "stats.incrementQueued();")
    with open(uf, "w") as file:
        file.write(ucontent)

print("Fixed final errors")

