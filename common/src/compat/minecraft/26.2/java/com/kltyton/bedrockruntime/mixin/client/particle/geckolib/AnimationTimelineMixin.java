package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.cache.animation.keyframeevent.KeyFrameData;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import com.geckolib.renderer.base.GeoRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.AnimationMarkerScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.AnimationFrameContext;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** Dispatches particle effects through GeckoLib's ordered keyframe handlers. */
@Pseudo
@Mixin(targets = "com.geckolib.animation.state.AnimationTimeline", remap = false)
public abstract class AnimationTimelineMixin {
    @WrapMethod(method = "triggerKeyframeMarkersBetween")
    private <T extends GeoAnimatable> void kltyton_bedrock_runtime$withMarkerSamples(T animatable, GeoRenderState renderState,
            double fromTime, double toTime, AnimationController<T> controller,
            AnimationController.KeyframeEventHandler<T, SoundKeyframeData> soundHandler,
            AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleHandler,
            AnimationController.KeyframeEventHandler<T, CustomInstructionKeyframeData> customHandler,
            Operation<Void> original) {
        try (AnimationMarkerScope ignored = AnimationMarkerScope.enter(toTime < fromTime)) {
            original.call(animatable, renderState, fromTime, toTime, controller, soundHandler, particleHandler, customHandler);
        }
    }

    @WrapMethod(method = "getKeyframesForAnimation")
    private <M extends KeyFrameData> List<M> kltyton_bedrock_runtime$captureMarkerSamples(double startTime, double endTime,
            M[] markers, Operation<List<M>> original) {
        List<M> selected = original.call(startTime, endTime, markers);
        if (markers instanceof ParticleKeyframeData[]) AnimationMarkerScope.record(selected.size(), startTime, endTime);
        return selected;
    }

    @ModifyVariable(method = "triggerKeyframeMarkersBetween", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private <T extends GeoAnimatable> AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> kltyton_bedrock_runtime$wrapParticleHandler(
            @Nullable AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> original) {
        return event -> {
            double animationTime = AnimationMarkerScope.nextSample();
            var entity = GeckoLibHelper.animationEntity(event.animatable(), event.renderState());
            if (entity != null) IEntity.of(entity).kltyton_bedrock_runtime$setAnimationTime((float) animationTime);
            try (AnimationFrameContext ignored = AnimationFrameContext.enter(animationTime)) {
                if (original != null) original.handle(event);
                PSDiagnostics.infoFirstN("geckolib-particle-keyframe-dispatch", 20,
                        "GeckoLib particle keyframe dispatch effect={} locator={}",
                        event.keyframeData().getEffect(), event.keyframeData().getLocatorName());
                GeckoLibHelper.processParticleEffect(event);
            }
        };
    }
}
