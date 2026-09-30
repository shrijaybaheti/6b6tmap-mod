import os

replacements = {
    'net.fabricmc.fabric.api.client.command.v2.ClientCommandManager': 'net.fabricmc.fabric.api.client.command.v2.ClientCommands',
    'net.fabricmc.fabric.api.client.command.v2.net.fabricmc.fabric.api.client.command.v2.ClientCommands': 'net.fabricmc.fabric.api.client.command.v2.ClientCommands',
    'import net.minecraft.ChatChatFormatting;': 'import net.minecraft.ChatFormatting;',
    'ChatChatFormatting': 'ChatFormatting',
    'client.player.chunkPosition().x;': 'client.player.chunkPosition().x();',
    'client.player.chunkPosition().z;': 'client.player.chunkPosition().z();'
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
