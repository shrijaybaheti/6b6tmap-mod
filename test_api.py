import os
import zipfile

def search_class(cache_dir, target_class):
    for root, dirs, files in os.walk(cache_dir):
        for file in files:
            if file.endswith('.jar'):
                jar_path = os.path.join(root, file)
                try:
                    with zipfile.ZipFile(jar_path, 'r') as jar:
                        if target_class in jar.namelist():
                            print(f"Found in {jar_path}")
                            jar.extract(target_class, 'dump')
                            return
                except:
                    pass

search_class(os.path.expanduser('~/.gradle/caches/fabric-loom'), 'net/minecraft/network/packet/s2c/play/ChunkDataS2CPacket.class')
