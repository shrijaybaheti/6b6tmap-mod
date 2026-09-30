import os

replacements = {
    'LevelLevelChunkSection': 'LevelChunkSection',
    'WorldChunk': 'LevelChunk',
    'Component.of(': 'Component.literal(',
    '.formatted(': '.withStyle(',
    'world.dimension().getValue()': 'world.dimension().location()',
    'section.isEmpty()': 'section.hasOnlyAir()'
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
