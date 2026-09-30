import os
import re

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
    'Registries.BLOCK': 'BuiltInRegistries.BLOCK',
    'net.minecraft.block.Block': 'net.minecraft.world.level.block.Block',
    'Component.of(': 'Component.literal(',
    '.getSectionArray()': '.getSections()',
    'chunk.getBottomY()': 'chunk.getMinBuildHeight()'
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
