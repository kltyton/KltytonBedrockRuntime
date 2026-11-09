package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the base model and keeps all layer callbacks on the renderer's interpolation time. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void kltyton_bedrock_runtime$captureTime(LivingEntity entity, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        RenderFrameContext.enter(partialTick);
        try { original.call(entity, yaw, partialTick, pose, buffers, light); }
        finally { RenderFrameContext.exit(); }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void kltyton_bedrock_runtime$captureModel(EntityModel<?> model, PoseStack pose, VertexConsumer buffer,
            int light, int overlay, float red, float green, float blue, float alpha, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        VanillaModelCapture.render(entity, () -> original.call(model, pose, buffer, light, overlay, red, green, blue, alpha));
    }

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void kltyton_bedrock_runtime$localDamageFlash(LivingEntity entity, float white, CallbackInfoReturnable<Integer> callback) {
        if (BonePartSelectionClient.shouldSuppressFullEntityRed(entity)) {
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false)));
        }
    }
}
