package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> {
    @Shadow
    protected @Nullable AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleKeyframeHandler;
    @Shadow
    @Final
    protected String name;

    @Shadow
    public abstract double getCurrentAnimationTime();

    @Unique
    private AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> kltyton_bedrock_runtime$wrappedParticleHandler;

    @Inject(method = "checkControllerState", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$ensureParticleHandler(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, GeoModel<T> geoModel, CallbackInfoReturnable<Boolean> cir) {
        kltyton_bedrock_runtime$wrapParticleHandler();
    }

    @Inject(method = "checkControllerState", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$captureAnimationTime(T animatable, GeoRenderState renderState,
            AnimatableManager<T> manager, GeoModel<T> geoModel, CallbackInfoReturnable<Boolean> cir) {
        var entity = GeckoLibHelper.animationEntity(animatable, renderState);
        if (entity != null && cir.getReturnValue()) {
            IEntity.of(entity).kltyton_bedrock_runtime$setAnimationTime((float) getCurrentAnimationTime());
        }
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$wrapCustomParticleHandler(AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleHandler, CallbackInfoReturnable<AnimationController<T>> cir) {
        kltyton_bedrock_runtime$wrapParticleHandler();
    }

    @Unique
    private void kltyton_bedrock_runtime$wrapParticleHandler() {
        if (kltyton_bedrock_runtime$wrappedParticleHandler != null && particleKeyframeHandler == kltyton_bedrock_runtime$wrappedParticleHandler) {
            return;
        }

        AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> original = particleKeyframeHandler;
        kltyton_bedrock_runtime$wrappedParticleHandler = event -> {
            if (original != null) {
                original.handle(event);
            }
        };
        particleKeyframeHandler = kltyton_bedrock_runtime$wrappedParticleHandler;
        PSDiagnostics.infoOnce("geckolib-animation-controller:" + name, "GeckoLib AnimationController particle handler hooked controller={}", name);
    }
}