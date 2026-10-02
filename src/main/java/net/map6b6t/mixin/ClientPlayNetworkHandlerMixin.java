package net.map6b6t.mixin;

import net.map6b6t.SpawnMapMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void afterChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        SpawnMapMod.getInstance().handleRawPacket(packet.getChunkX(), packet.getChunkZ(), packet);
    }
}
