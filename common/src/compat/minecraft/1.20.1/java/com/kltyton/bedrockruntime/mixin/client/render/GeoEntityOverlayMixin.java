package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoEntityRenderer", remap = false)
public abstract class GeoEntityOverlayMixin {
    @Shadow protected Entity animatable;

    @Inject(method = "getPackedOverlay", at = @At("RETURN"), cancellable = true)
    private void kltyton_bedrock_runtime$partDamageOverlay(CallbackInfoReturnable<Integer> callback) {
        if (animatable instanceof LivingEntity living && BonePartSelectionClient.shouldSuppressFullEntityRed(living)) {
            callback.setReturnValue(OverlayTexture.pack(callback.getReturnValue() & 65535, OverlayTexture.v(false)));
        }
    }
}
