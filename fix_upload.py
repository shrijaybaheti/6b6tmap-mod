import os, glob, re

base_dir = r"D:\projects\6b6tmapprebeta\Minecraft_Mods\versions"

for ver_dir in glob.glob(os.path.join(base_dir, "*")):
    if not os.path.isdir(ver_dir): continue
    
    upload_path = os.path.join(ver_dir, "src/main/java/net/map6b6t/network/UploadService.java")
    if os.path.exists(upload_path):
        with open(upload_path, "r", encoding="utf-8") as f: content = f.read()
        
        content = re.sub(
            r'public void submitAsyncScan\(\s*String dimension,\s*int chunkX,\s*int chunkZ,\s*String playerName,\s*String serverVersion,\s*(?:List<ScannedBlock>\s+scannedBlocks|net\.map6b6t\.scanner\.PrimitiveChunkSnapshot\s+snapshot),',
            r'public void submitAsyncScan(\n            String dimension,\n            net.minecraft.world.level.chunk.LevelChunk chunk,\n            int chunkX,\n            int chunkZ,\n            String playerName,\n            String serverVersion,',
            content
        )
        
        with open(upload_path, "w", encoding="utf-8") as f: f.write(content)

print("Done")
