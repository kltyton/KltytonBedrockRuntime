package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.geckolib.renderer.base.RenderPassInfo", remap = false)
public abstract class RenderPassInfoMixin {
    @Inject(method = "create", at = @At("RETURN"))
    private static <R extends GeoRenderState> void kltyton_bedrock_runtime$attachLocatorListeners(GeoRenderer<?, ?, R> renderer, R renderState, PoseStack poseStack, CameraRenderState cameraState, boolean willRender, CallbackInfoReturnable<RenderPassInfo<R>> cir) {
        GeckoLibHelper.attachLocatorListeners(renderState, cir.getReturnValue());
    }

    @WrapMethod(method = "renderPosed", remap = false)
    private void kltyton_bedrock_runtime$renderPosed(Runnable renderTask, Operation<Void> original) {
        RenderPassInfo<?> renderPass = (RenderPassInfo<?>) (Object) this;
        GeckoLibHelper.enterRenderPass(renderPass);
        try {
            original.call(renderTask);
        } finally {
            GeckoLibHelper.exitRenderPass(renderPass);
        }
    }
}
