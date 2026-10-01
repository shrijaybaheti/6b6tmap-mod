package net.map6b6t.mixin;

import net.map6b6t.SpawnMapMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void afterChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;
        if (handler.getWorld() == null) return;
        
        // Grab the chunk AFTER Minecraft has natively parsed the packet.
        WorldChunk chunk = handler.getWorld().getChunk(packet.getX(), packet.getZ());
        if (chunk != null) {
            SpawnMapMod.getInstance().handleIncomingServerChunk(chunk);
        }
    }
}
