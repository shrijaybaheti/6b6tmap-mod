import os, glob, re

base_dir = r"D:\projects\6b6tmapprebeta\Minecraft_Mods\versions"

for ver_dir in glob.glob(os.path.join(base_dir, "*")):
    if not os.path.isdir(ver_dir): continue
    
    # Standardize UploadService.java signature
    upload_path = os.path.join(ver_dir, "src/main/java/net/map6b6t/network/UploadService.java")
    if os.path.exists(upload_path):
        with open(upload_path, "r", encoding="utf-8") as f: content = f.read()
        
        # Remove any existing signature variations and set it to the standard one
        content = re.sub(
            r'public void submitAsyncScan\([\s\S]*?java\.util\.function\.BiConsumer<Long,\s*Integer>\s*onHashComputed\s*\)',
            r'public void submitAsyncScan(\n            String dimension,\n            net.minecraft.world.level.chunk.LevelChunk chunk,\n            int chunkX,\n            int chunkZ,\n            String playerName,\n            String serverVersion,\n            java.util.function.BiConsumer<Long, Integer> onHashComputed\n    )',
            content
        )
        
        with open(upload_path, "w", encoding="utf-8") as f: f.write(content)

print("Done")
