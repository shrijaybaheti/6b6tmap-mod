package net.map6b6t.mixin;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.gui.NametagCounts;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class PlayerUploadNametagMixin {
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Text map6b6t$appendUploadCount(Text name) {
        if (!ConfigManager.get().showNametagHud || name == null) return name;
        Integer count = NametagCounts.get(name.getString());
        return count == null ? name : name.copy().append(Text.literal("\n" + count + " chunks uploaded"));
    }
}
