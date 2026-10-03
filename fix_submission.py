
import os

f = "src/main/java/net/map6b6t/network/ChunkSubmission.java"
with open(f, "r") as file:
    content = file.read()

content = content.replace("public final byte[] rawBytes;", "public final net.map6b6t.scanner.PrimitiveChunkSnapshot blocks;")
content = content.replace("byte[] rawBytes,", "net.map6b6t.scanner.PrimitiveChunkSnapshot blocks,")
content = content.replace("this.rawBytes = rawBytes;", "this.blocks = blocks;")
content = content.replace("java.util.Arrays.hashCode(rawBytes);", "blocks.hashCode();") # PrimitiveChunkSnapshot has hashCode

with open(f, "w") as file:
    file.write(content)
print("Fixed ChunkSubmission")

