package net.map6b6t.mixin;
import net.map6b6t.SpawnMapMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void afterChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        ClientPacketListener handler = (ClientPacketListener) (Object) this;
        if (handler.getLevel() == null) return;
        LevelChunk chunk = handler.getLevel().getChunk(packet.getX(), packet.getZ());
        if (chunk != null) { SpawnMapMod.getInstance().handleIncomingServerChunk(chunk); }
    }
}
