package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CN: 可用时用 KltytonBedrockRuntime 的部位局部闪红替代原版全身红色受击 overlay。
 * EN: Replaces vanilla full-body red hurt overlay with KltytonBedrockRuntime's part-local flash when available.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.LivingEntityRenderer")
public abstract class LivingEntityRendererMixin {
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void kltyton_bedrock_runtime$suppressFullRedOverlay(LivingEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector renderTasks, CameraRenderState camera, CallbackInfo callbackInfo) {
        if (state.hasRedOverlay && BonePartSelectionClient.shouldSuppressFullEntityRed(state)) {
            state.hasRedOverlay = false;
        }
    }
}
