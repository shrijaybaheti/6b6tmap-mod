package net.map6b6t.mixin;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.gui.NametagCounts;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class PlayerUploadNametagMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void map6b6t$appendUploadCount(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!ConfigManager.get().showNametagHud || !(entity instanceof Player player)) return;
        Integer count = NametagCounts.get(player.getName().getString());
        if (count != null) cir.setReturnValue(cir.getReturnValue().copy().append(Component.literal("\n" + count + " chunks uploaded")));
    }
}
