
import os

f = "src/main/java/net/map6b6t/network/ChunkSubmission.java"
with open(f, "r") as file:
    content = file.read()

content = content.replace("public final net.map6b6t.scanner.PrimitiveChunkSnapshot blocks;", "public final Object blocks;")
content = content.replace("net.map6b6t.scanner.PrimitiveChunkSnapshot blocks,", "Object blocks,")
content = content.replace("blocks.hashCode();", "blocks != null ? blocks.hashCode() : 0;")

with open(f, "w") as file:
    file.write(content)
print("Fixed ChunkSubmission to use Object blocks")

