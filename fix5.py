import os

replacements = {
    'MinecraftClient': 'Minecraft',
    'ClientPlayerEntity': 'LocalPlayer',
    'Formatting': 'ChatFormatting',
    'net.minecraft.world.level.net.map6b6t.EnvBridge': 'net.map6b6t.EnvBridge',
    'ClientCommandManager': 'net.fabricmc.fabric.api.client.command.v2.ClientCommandManager',
    'BuiltInRegistries.BLOCK.getId(block)': 'BuiltInRegistries.BLOCK.getKey(block)',
    'net.minecraft.util.Formatting': 'net.minecraft.ChatFormatting',
    'net.fabricmc.fabric.api.client.command.v2.net.fabricmc.fabric.api.client.command.v2.ClientCommandManager': 'net.fabricmc.fabric.api.client.command.v2.ClientCommandManager',
    'import net.minecraft.ChatFormatting;': 'import net.minecraft.ChatFormatting;',
    'import net.minecraft.client.Minecraft;': 'import net.minecraft.client.Minecraft;'
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
