import os

replacements = {
    'pos.x': 'net.map6b6t.EnvBridge.getChunkX(pos)',
    'pos.z': 'net.map6b6t.EnvBridge.getChunkZ(pos)',
    'chunk.getMinBuildHeight()': 'net.map6b6t.EnvBridge.getMinBuildHeight(chunk)',
    'chunk.getBottomY()': 'net.map6b6t.EnvBridge.getMinBuildHeight(chunk)',
    'client.getCurrentServerEntry() != null && client.getCurrentServerEntry().version != null': 'net.map6b6t.EnvBridge.getServerVersion(client) != null',
    'client.getCurrentServer() != null && client.getCurrentServer().version != null': 'net.map6b6t.EnvBridge.getServerVersion(client) != null',
    'client.getCurrentServerEntry().version.getString()': 'net.map6b6t.EnvBridge.getServerVersion(client)',
    'client.getCurrentServer().version.getString()': 'net.map6b6t.EnvBridge.getServerVersion(client)',
    'client.getCurrentServerEntry().address': 'net.map6b6t.EnvBridge.getServerAddress(client)',
    'client.getCurrentServer().address': 'net.map6b6t.EnvBridge.getServerAddress(client)',
    'client.getCurrentServerEntry() == null': 'net.map6b6t.EnvBridge.getServerAddress(client) == null',
    'client.getCurrentServer() == null': 'net.map6b6t.EnvBridge.getServerAddress(client) == null',
    'client.getNetworkHandler() != null && client.getNetworkHandler().getBrand() != null': 'net.map6b6t.EnvBridge.getServerBrand(client) != null',
    'client.getConnection() != null && client.getConnection().getBrand() != null': 'net.map6b6t.EnvBridge.getServerBrand(client) != null',
    'client.getNetworkHandler().getBrand()': 'net.map6b6t.EnvBridge.getServerBrand(client)',
    'client.getConnection().getBrand()': 'net.map6b6t.EnvBridge.getServerBrand(client)',
    'client.player.getChunkPos()': 'client.player.chunkPosition()',
    'world.getRegistryKey()': 'world.dimension()',
    'ChunkPos.toLong(': 'ChunkPos.asLong(',
    'BuiltInBuiltInRegistries': 'BuiltInRegistries',
    'fluidState.getFluid()': 'fluidState.getType()'
}

directory = 'd:/projects/6b6tmapprebeta/Minecraft_Mods/versions/26.x/src/main/java/net/map6b6t'

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            for k, v in replacements.items():
                content = content.replace(k, v)
                
            with open(filepath, 'w') as f:
                f.write(content)

print("Done")
