import os

replacements = {
    'net.minecraft.client.MinecraftClient': 'net.minecraft.client.Minecraft',
    'net.minecraft.client.network.ClientPlayerEntity': 'net.minecraft.client.player.LocalPlayer',
    'net.minecraft.client.network.LocalPlayer': 'net.minecraft.client.player.LocalPlayer',
    'net.minecraft.client.world.ClientWorld': 'net.minecraft.client.multiplayer.ClientLevel',
    'net.minecraft.client.world.ClientLevel': 'net.minecraft.client.multiplayer.ClientLevel',
    'net.minecraft.text.Text': 'net.minecraft.network.chat.Component',
    'net.minecraft.Component.Component': 'net.minecraft.network.chat.Component',
    'net.minecraft.world.chunk.WorldChunk': 'net.minecraft.world.level.chunk.LevelChunk',
    'net.minecraft.world.chunk.LevelChunk': 'net.minecraft.world.level.chunk.LevelChunk',
    'net.minecraft.world.chunk.ChunkSection': 'net.minecraft.world.level.chunk.LevelChunkSection',
    'net.minecraft.util.math.ChunkPos': 'net.minecraft.world.level.ChunkPos',
    'net.minecraft.util.Formatting': 'net.minecraft.ChatFormatting',
    'net.minecraft.block.BlockState': 'net.minecraft.world.level.block.state.BlockState',
    'net.minecraft.fluid.FluidState': 'net.minecraft.world.level.material.FluidState',
    'net.minecraft.fluid.Fluids': 'net.minecraft.world.level.material.Fluids',
    'net.minecraft.registry.Registries': 'net.minecraft.core.registries.BuiltInRegistries',
    'net.minecraft.block.Block': 'net.minecraft.world.level.block.Block',
    'Component.of(': 'Component.literal(',
    '.getSectionArray()': '.getSections()',
    'chunk.getBottomY()': 'net.map6b6t.EnvBridge.getMinBuildHeight(chunk)',
    'chunk.getMinBuildHeight()': 'net.map6b6t.EnvBridge.getMinBuildHeight(chunk)',
    'pos.x': 'net.map6b6t.EnvBridge.getChunkX(pos)',
    'pos.z': 'net.map6b6t.EnvBridge.getChunkZ(pos)',
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
    'ChunkPos.toLong(': 'net.map6b6t.EnvBridge.asLong(',
    'ChunkPos.asLong(': 'net.map6b6t.EnvBridge.asLong(',
    'BuiltInBuiltInRegistries': 'BuiltInRegistries',
    'fluidState.getFluid()': 'fluidState.getType()',
    'Registries.BLOCK': 'BuiltInRegistries.BLOCK'
}

directory = 'd:/projects/6b6tmapprebeta/Minecraft_Mods/versions/26.x/src/main/java/net/map6b6t'

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java') and file != 'EnvBridge.java':
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            for k, v in replacements.items():
                content = content.replace(k, v)
                
            with open(filepath, 'w') as f:
                f.write(content)

print("Done")
