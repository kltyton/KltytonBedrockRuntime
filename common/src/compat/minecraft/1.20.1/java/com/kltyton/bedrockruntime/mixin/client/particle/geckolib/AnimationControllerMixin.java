package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeLocation;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimationController;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.lang.ref.WeakReference;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements IPSAnimationController {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected AnimationController.State animationState;
    @Shadow
    protected AnimationController.ParticleKeyframeHandler<T> particleKeyframeHandler;

    @Unique
    private AnimationController.ParticleKeyframeHandler<T> kltyton_bedrock_runtime$wrappedParticleHandler;

    @Unique
    private List<GeoBone> kltyton_bedrock_runtime$bonesWhichHasLocators;

    @Unique
    private Runnable kltyton_bedrock_runtime$reloadCallback;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$registerReloadCallback(CallbackInfo ci) {
        WeakReference<AnimationControllerMixin<T>> controllerRef = new WeakReference<>(this);
        kltyton_bedrock_runtime$reloadCallback = () -> {
            AnimationControllerMixin<T> controller = controllerRef.get();
            if (controller != null) controller.kltyton_bedrock_runtime$bonesWhichHasLocators = null;
        };
        GeckoLibHelper.addWeakReloadCallback(kltyton_bedrock_runtime$reloadCallback);
    }

    @Override
    public List<GeoBone> kltyton_bedrock_runtime$getBonesWhichHasLocators() {
        return Objects.requireNonNullElse(kltyton_bedrock_runtime$bonesWhichHasLocators, List.of());
    }

    @Override
    public void kltyton_bedrock_runtime$setBonesWhichHasLocators(Collection<CoreGeoBone> registeredBones) {
        if (kltyton_bedrock_runtime$bonesWhichHasLocators == null) {
            this.kltyton_bedrock_runtime$bonesWhichHasLocators = registeredBones.stream().filter(GeoBone.class::isInstance).map(GeoBone.class::cast).filter(bone -> {
                Map<String, LocatorValue> locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
                return locators != null && !locators.isEmpty();
            }).toList();
        }
    }

    @WrapMethod(method = "process")
    private void kltyton_bedrock_runtime$withRenderedItem(CoreGeoModel<T> model, AnimationState<T> state,
            Map<?, ?> bones, Map<?, ?> snapshots, double seekTime, boolean crashWhenCantFindBone,
            Operation<Void> original) {
        var item = state.getData(DataTickets.ITEMSTACK);
        if (item == null && animatable instanceof GeoItem) item = RenderItemContext.currentItem();
        try (RenderItemContext ignored = RenderItemContext.enter(item, RenderItemContext.currentOwner(),
                RenderItemContext.currentDisplay())) {
            original.call(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        }
    }

    @Inject(method = "process", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$ensureParticleHandler(CallbackInfo ci) {
        kltyton_bedrock_runtime$wrapParticleHandler();
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$wrapCustomParticleHandler(AnimationController.ParticleKeyframeHandler<T> handler, CallbackInfoReturnable<AnimationController<T>> cir) {
        kltyton_bedrock_runtime$wrapParticleHandler();
    }

    @Unique
    private void kltyton_bedrock_runtime$wrapParticleHandler() {
        if (kltyton_bedrock_runtime$wrappedParticleHandler != null && particleKeyframeHandler == kltyton_bedrock_runtime$wrappedParticleHandler) return;
        AnimationController.ParticleKeyframeHandler<T> original = particleKeyframeHandler;
        kltyton_bedrock_runtime$wrappedParticleHandler = event -> {
            ActorAnimationContext.recordAnimationTime(event.getAnimatable(), (float) (event.getAnimationTick() / 20.0));
            if (original != null) original.handle(event);
            GeckoLibHelper.processParticleEffect(event.getAnimatable(), event.getController(), event.getKeyframeData());
        };
        particleKeyframeHandler = kltyton_bedrock_runtime$wrappedParticleHandler;
    }

    @Inject(method = "process", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/core/molang/MolangParser;setValue(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"))
    private void kltyton_bedrock_runtime$recordTransitionTime(CallbackInfo ci) {
        ActorAnimationContext.recordAnimationTime(animatable, 0.0F);
    }

    @Inject(method = "processCurrentAnimation", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/core/molang/MolangParser;setMemoizedValue(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"))
    private void kltyton_bedrock_runtime$recordAnimationTime(double adjustedTick, double seekTime, boolean crashWhenCantFindBone,
            CallbackInfo ci, @Local(index = 6) double finalAdjustedTick) {
        ActorAnimationContext.recordAnimationTime(animatable, (float) (finalAdjustedTick / 20.0));
    }

    @Inject(method = "getCurrentKeyFrameLocation", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$recordKeyframeLerp(List<Keyframe<?>> frames, double ageInTicks,
            CallbackInfoReturnable<KeyframeLocation<Keyframe<?>>> cir) {
        double totalFrameTime = 0.0;
        for (Keyframe<?> frame : frames) {
            totalFrameTime += frame.length();
            if (totalFrameTime > ageInTicks) break;
        }
        ActorAnimationContext.recordKeyframeLerp(animatable, frames, ageInTicks, totalFrameTime,
                cir.getReturnValue().keyframe());
    }

    @Inject(method = "resetEventKeyFrames", at = @At("HEAD"))
    private void removeEmitters(CallbackInfo ci) {
        GeckoLibHelper.removeEmittersWhenAnimationChange(animationState, (AnimationController<?>) (Object) this, animatable);
    }
}
