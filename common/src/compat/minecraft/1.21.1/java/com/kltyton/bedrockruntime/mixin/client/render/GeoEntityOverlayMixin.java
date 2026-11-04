package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoEntityRenderer", remap = false)
public abstract class GeoEntityOverlayMixin {
    @Inject(method = "getPackedOverlay", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$partDamageOverlay(@Coerce Object animatable, float white, float partialTick,
            CallbackInfoReturnable<Integer> callback) {
        if (animatable instanceof LivingEntity living && BonePartSelectionClient.shouldSuppressFullEntityRed(living)) {
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false)));
        }
    }
}
