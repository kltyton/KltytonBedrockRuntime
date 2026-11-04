package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.geckolib.animation.state.ControllerState;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRendererInternals;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kltyton.bedrockruntime.client.compat.geckolib.animation.GraphAnimationLayer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inserts an opted-in graph without evaluating native controllers or MoLang expressions twice. */
@Mixin(value = GeoRendererInternals.class, remap = false)
public interface GeckoGraphCompositionMixin {
    @WrapOperation(method = "applyAnimationControllers", at = @At(value = "INVOKE",
            target = "Lcom/geckolib/animation/AnimationProcessor;createBoneSnapshots(Lcom/geckolib/animation/state/ControllerState;Lcom/geckolib/renderer/base/BoneSnapshots;)V"))
    private void kltyton_bedrock_runtime$composeGraph(ControllerState state, BoneSnapshots snapshots, Operation<Void> original,
                                           @Local(argsOnly = true) RenderPassInfo<?> pass,
                                           @Share("graphApplied") LocalBooleanRef applied) {
        if (state.additive() && !applied.get()) {
            GraphAnimationLayer.applyGraphPose(pass, snapshots);
            applied.set(true);
        }
        original.call(state, snapshots);
    }

    @Inject(method = "applyAnimationControllers", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$finishGraph(RenderPassInfo<?> pass, BoneSnapshots snapshots, CallbackInfo callback,
                                          @Share("graphApplied") LocalBooleanRef applied) {
        if (!applied.get()) {
            GraphAnimationLayer.applyGraphPose(pass, snapshots);
        }
    }
}
