import os, glob

base_dir = r"D:\projects\6b6tmapprebeta\Minecraft_Mods\versions"

for ver_dir in glob.glob(os.path.join(base_dir, "*")):
    if not os.path.isdir(ver_dir): continue
    
    spawn_path = os.path.join(ver_dir, "src/main/java/net/map6b6t/SpawnMapMod.java")
    if os.path.exists(spawn_path):
        with open(spawn_path, "r", encoding="utf-8") as f: content = f.read()
        content = content.replace("playerName, serverVer, snapshot, (k, v)", "playerName, serverVer, (k, v)")
        with open(spawn_path, "w", encoding="utf-8") as f: f.write(content)

print("Done")
