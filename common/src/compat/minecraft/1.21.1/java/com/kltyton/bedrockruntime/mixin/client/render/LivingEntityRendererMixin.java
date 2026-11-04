package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void kltyton_bedrock_runtime$captureModel(EntityModel<?> model, PoseStack pose, VertexConsumer buffer,
            int light, int overlay, int color, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        VanillaModelCapture.render(entity, () -> original.call(model, pose, buffer, light, overlay, color));
    }

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$localDamageFlash(LivingEntity entity, float white, CallbackInfoReturnable<Integer> callback) {
        if (BonePartSelectionClient.shouldSuppressFullEntityRed(entity)) {
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false)));
        }
    }
}
