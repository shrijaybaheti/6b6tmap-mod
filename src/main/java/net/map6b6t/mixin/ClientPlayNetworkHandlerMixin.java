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
    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void beforeChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        SpawnMapMod.getInstance().markChunkReceived(packet.getChunkX(), packet.getChunkZ());
    }
}
