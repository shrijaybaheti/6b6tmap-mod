import os

replacements = {
    'ChunkPos.toLong(': 'ChunkPos.asLong(',
    'getCurrentServerEntry()': 'getCurrentServer()',
    'getNetworkHandler()': 'getConnection()',
    'world.getRegistryKey()': 'world.dimension()',
    'net.minecraft.block.BlockState': 'net.minecraft.world.level.block.state.BlockState',
    'net.minecraft.fluid.FluidState': 'net.minecraft.world.level.material.FluidState',
    'net.minecraft.fluid.Fluids': 'net.minecraft.world.level.material.Fluids',
    'net.minecraft.registry.Registries': 'net.minecraft.core.registries.BuiltInRegistries',
    'net.minecraft.block.Block': 'net.minecraft.world.level.block.Block',
    'Registries.BLOCK': 'BuiltInRegistries.BLOCK'
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
