package net.map6b6t.mixin;

import net.map6b6t.SpawnMapMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"))
    private void beforeChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        SpawnMapMod.getInstance().markChunkReceived(packet.getX(), packet.getZ());
    }
}
