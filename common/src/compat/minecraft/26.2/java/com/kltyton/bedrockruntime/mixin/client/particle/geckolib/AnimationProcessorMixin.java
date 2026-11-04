package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.animation.object.EasingType;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records the channel ratio before its keyframe expressions run, preserving GeckoLib interpolation. */
@Pseudo
@Mixin(targets = "com.geckolib.animation.AnimationProcessor", remap = false)
public abstract class AnimationProcessorMixin {
    @Inject(method = "findAnimationPointValue", at = @At(value = "INVOKE",
            target = "Lcom/geckolib/loading/math/MathValue;get(Lcom/geckolib/animation/state/ControllerState;)D",
            ordinal = 0))
    private static void kltyton_bedrock_runtime$captureKeyframeLerp(BoneSnapshot snapshot, ControllerState state,
            AnimationPoint animation, @Nullable AnimationPoint previous, int boneIndex,
            AnimationPoint.Transform transform, AnimationPoint.Axis axis, @Nullable EasingType easing,
            CallbackInfoReturnable<Float> cir) {
        ActorAnimationContext.recordKeyframeLerp(state, animation, boneIndex, transform, axis);
    }
}
