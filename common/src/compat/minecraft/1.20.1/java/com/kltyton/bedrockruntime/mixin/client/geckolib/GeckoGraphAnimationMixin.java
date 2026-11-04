package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.kltyton.bedrockruntime.client.compat.geckolib.animation.GraphAnimationBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Applies graph poses after native GeckoLib4 controller processing and before bone rendering. */
@Mixin(value = GeoModel.class, remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GeckoGraphAnimationMixin {
    @Accessor("currentModel")
    protected abstract BakedGeoModel kltyton_bedrock_runtime$currentModel();

    @Inject(method = "handleAnimations", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$applyGraphAnimation(GeoAnimatable animatable, long instanceId,
            AnimationState animationState, CallbackInfo callbackInfo) {
        GraphAnimationBridge.apply((GeoModel<?>) (Object) this, animatable, instanceId,
                this.kltyton_bedrock_runtime$currentModel(), animationState, animationState.getPartialTick());
    }
}
