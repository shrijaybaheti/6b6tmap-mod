import os

filepath = 'd:/projects/6b6tmapprebeta/Minecraft_Mods/src/main/java/net/map6b6t/network/ChunkUploader.java'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('return value.replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\\"");',
                          'return value.replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r").replace("\\t", "\\\\t");')

with open(filepath, 'w') as f:
    f.write(content)
print("Done")
