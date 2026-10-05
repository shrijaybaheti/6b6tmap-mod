package net.map6b6t.mixin;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"))
    private void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet, CallbackInfo info) {
        net.map6b6t.SpawnMapMod.getInstance().markChunkReceived(packet.getX(), packet.getZ());
    }
}
