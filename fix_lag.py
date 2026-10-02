import os, glob, re

base_dir = r"D:\projects\6b6tmapprebeta\Minecraft_Mods\versions"

for ver_dir in glob.glob(os.path.join(base_dir, "*")):
    if not os.path.isdir(ver_dir): continue
    
    spawn_path = os.path.join(ver_dir, "src/main/java/net/map6b6t/SpawnMapMod.java")
    if os.path.exists(spawn_path):
        with open(spawn_path, "r", encoding="utf-8") as f: content = f.read()
        content = re.sub(r'List<ScannedBlock>\s+snapshot\s*=\s*ChunkScanner\.snapshotAndScan\(chunk\);\s*(UploadService\.get\(\)\.submitAsyncScan\(dimension,\s*)net\.map6b6t\.EnvBridge\.getChunkX', r'\1chunk, net.map6b6t.EnvBridge.getChunkX', content)
        content = re.sub(r'net\.map6b6t\.scanner\.PrimitiveChunkSnapshot\s+snapshot\s*=\s*ChunkScanner\.snapshotAndScan\(chunk\);\s*(UploadService\.get\(\)\.submitAsyncScan\(dimension,\s*)net\.map6b6t\.EnvBridge\.getChunkX', r'\1chunk, net.map6b6t.EnvBridge.getChunkX', content)
        with open(spawn_path, "w", encoding="utf-8") as f: f.write(content)

    upload_path = os.path.join(ver_dir, "src/main/java/net/map6b6t/network/UploadService.java")
    if os.path.exists(upload_path):
        with open(upload_path, "r", encoding="utf-8") as f: content = f.read()
        content = re.sub(r'(public void submitAsyncScan\([\s\S]*?String serverVersion,\s*)List<ScannedBlock>\s+scannedBlocks', r'\1net.minecraft.world.level.chunk.LevelChunk chunk', content)
        content = re.sub(r'(public void submitAsyncScan\([\s\S]*?String serverVersion,\s*)net\.map6b6t\.scanner\.PrimitiveChunkSnapshot\s+snapshot', r'\1net.minecraft.world.level.chunk.LevelChunk chunk', content)
        
        content = re.sub(r'(scanExecutor\.execute\(\(\)\s*->\s*\{\s*try\s*\{\s*)if\s*\(scannedBlocks\s*==\s*null\s*\|\|\s*scannedBlocks\.isEmpty\(\)\)', r'\1java.util.List<net.map6b6t.scanner.ScannedBlock> scannedBlocks = net.map6b6t.scanner.ChunkScanner.snapshotAndScan(chunk);\n                if (scannedBlocks == null || scannedBlocks.isEmpty())', content)
        content = re.sub(r'(scanExecutor\.execute\(\(\)\s*->\s*\{\s*try\s*\{\s*)if\s*\(snapshot\s*==\s*null\s*\|\|\s*snapshot\.size\s*==\s*0\)', r'\1net.map6b6t.scanner.PrimitiveChunkSnapshot snapshot = net.map6b6t.scanner.ChunkScanner.snapshotAndScan(chunk);\n                if (snapshot == null || snapshot.size == 0)', content)
        
        with open(upload_path, "w", encoding="utf-8") as f: f.write(content)
