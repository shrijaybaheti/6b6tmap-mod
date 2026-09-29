$files = Get-ChildItem -Recurse -File -Filter "*.java" src-26
foreach ($f in $files) {
    $content = Get-Content $f.FullName
    $content = $content -replace "net\.minecraft\.client\.MinecraftClient", "net.minecraft.client.Minecraft"
    $content = $content -replace "net\.minecraft\.client\.world\.ClientWorld", "net.minecraft.client.multiplayer.ClientLevel"
    $content = $content -replace "net\.minecraft\.client\.network\.ClientPlayerEntity", "net.minecraft.client.player.LocalPlayer"
    $content = $content -replace "net\.minecraft\.text\.Text", "net.minecraft.network.chat.Component"
    $content = $content -replace "net\.minecraft\.util\.math\.ChunkPos", "net.minecraft.world.level.ChunkPos"
    $content = $content -replace "net\.minecraft\.world\.chunk\.WorldChunk", "net.minecraft.world.level.chunk.LevelChunk"
    $content = $content -replace "net\.minecraft\.world\.chunk\.ChunkSection", "net.minecraft.world.level.chunk.LevelChunkSection"
    $content = $content -replace "net\.minecraft\.fluid\.FluidState", "net.minecraft.world.level.material.FluidState"
    $content = $content -replace "net\.minecraft\.fluid\.Fluids", "net.minecraft.world.level.material.Fluids"
    $content = $content -replace "net\.minecraft\.registry\.Registries", "net.minecraft.core.registries.BuiltInRegistries"
    $content = $content -replace "net\.minecraft\.block\.BlockState", "net.minecraft.world.level.block.state.BlockState"
    $content = $content -replace "net\.minecraft\.block\.Block", "net.minecraft.world.level.block.Block"
    $content = $content -replace "net\.minecraft\.util\.Formatting", "net.minecraft.ChatFormatting"
    
    $content = $content -replace "\bMinecraftClient\b", "Minecraft"
    $content = $content -replace "\bClientWorld\b", "ClientLevel"
    $content = $content -replace "\bClientPlayerEntity\b", "LocalPlayer"
    $content = $content -replace "\bText\.literal\b", "Component.literal"
    $content = $content -replace "\bText\b(?!\.)", "Component"
    $content = $content -replace "\bWorldChunk\b", "LevelChunk"
    $content = $content -replace "\bChunkSection\b", "LevelChunkSection"
    $content = $content -replace "\bRegistries\.BLOCK\.getId\b", "BuiltInRegistries.BLOCK.getKey"
    $content = $content -replace "\bRegistries\.BLOCK\b", "BuiltInRegistries.BLOCK"
    $content = $content -replace "\bgetSectionArray\b", "getSections"
    $content = $content -replace "\bgetBottomY\b", "getMinBuildHeight"
    $content = $content -replace "\bgetFluid\b", "getType"
    $content = $content -replace "\bFormatting\.", "ChatFormatting."
    
    $content | Set-Content $f.FullName
}
