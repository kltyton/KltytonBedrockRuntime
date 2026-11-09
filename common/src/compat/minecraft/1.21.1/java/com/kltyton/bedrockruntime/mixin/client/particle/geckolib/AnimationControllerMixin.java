package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimationController;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import com.kltyton.bedrockruntime.client.particle.access.IPSQueuedAnimation;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.runtime.geckolib.ActorAnimationContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.Keyframe;
import software.bernie.geckolib.animation.keyframe.KeyframeLocation;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.loading.json.raw.LocatorValue;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/** Connects GeckoLib controller timing, locator caches, and particle keyframes to Bedrock particles. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements IPSAnimationController {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected AnimationController.State animationState;
    @Shadow
    protected AnimationController.ParticleKeyframeHandler<T> particleKeyframeHandler;
    @Shadow
    protected QueuedAnimation currentAnimation;
    @Shadow
    protected Queue<QueuedAnimation> animationQueue;
    @Shadow
    protected RawAnimation currentRawAnimation;

    @Unique
    private List<GeoBone> kltyton_bedrock_runtime$bonesWhichHasLocators;
    @Unique
    private AnimationController.ParticleKeyframeHandler<T> kltyton_bedrock_runtime$wrappedParticleHandler;
    @Unique
    private WeakReference<Map<ResourceLocation, BakedAnimations>> kltyton_bedrock_runtime$animationResources;
    @Unique
    private RawAnimation kltyton_bedrock_runtime$unavailableSequence;

    @Override
    public List<GeoBone> kltyton_bedrock_runtime$getBonesWhichHasLocators() {
        return Objects.requireNonNullElse(kltyton_bedrock_runtime$bonesWhichHasLocators, List.of());
    }

    @Inject(method = "<init>(Lsoftware/bernie/geckolib/animatable/GeoAnimatable;Ljava/lang/String;ILsoftware/bernie/geckolib/animation/AnimationController$AnimationStateHandler;)V", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$registerReloadCallback(CallbackInfo ci) {
        GeckoLibHelper.<IPSAnimationController>addWeakReloadCallback(
                this, IPSAnimationController::kltyton_bedrock_runtime$clearLocatorBones);
    }

    @Override
    public void kltyton_bedrock_runtime$clearLocatorBones() {
        kltyton_bedrock_runtime$bonesWhichHasLocators = null;
    }

    @Override
    public void kltyton_bedrock_runtime$setBonesWhichHasLocators(Collection<GeoBone> registeredBones) {
        if (kltyton_bedrock_runtime$bonesWhichHasLocators == null) {
            this.kltyton_bedrock_runtime$bonesWhichHasLocators = registeredBones.stream().filter(bone -> {
                Map<String, LocatorValue> locators = IPSGeoBone.of(bone).kltyton_bedrock_runtime$getLocators();
                return locators != null && !locators.isEmpty();
            }).toList();
        }
    }

    @WrapMethod(method = "process")
    private void kltyton_bedrock_runtime$withRenderedItem(GeoModel<T> model, AnimationState<T> state,
            Map<?, ?> bones, Map<?, ?> snapshots, double seekTime, boolean crashWhenCantFindBone,
            Operation<Void> original) {
        var item = state.getData(DataTickets.ITEMSTACK);
        if (item == null && animatable instanceof GeoItem) item = RenderItemContext.currentItem();
        try (RenderItemContext ignored = RenderItemContext.enter(item, RenderItemContext.currentOwner(), RenderItemContext.currentDisplay())) {
            kltyton_bedrock_runtime$rebindAnimationResources(model);
            original.call(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        }
    }

    @Unique
    private void kltyton_bedrock_runtime$rebindAnimationResources(GeoModel<T> model) {
        Map<ResourceLocation, BakedAnimations> resources = GeckoLibCache.getBakedAnimations();
        if (kltyton_bedrock_runtime$animationResources != null
                && kltyton_bedrock_runtime$animationResources.get() == resources) {
            return;
        }

        if (kltyton_bedrock_runtime$animationResources != null) {
            // Rebind only the active and remaining stages. The controller owns time and event deduplication.
            QueuedAnimation active = kltyton_bedrock_runtime$rebindStage(model, currentAnimation);
            Queue<QueuedAnimation> remaining = new LinkedList<>();
            for (QueuedAnimation stage : animationQueue) {
                QueuedAnimation replacement = kltyton_bedrock_runtime$rebindStage(model, stage);
                if (replacement != null) {
                    remaining.add(replacement);
                }
            }

            if (active == null && remaining.isEmpty() && currentRawAnimation != null) {
                if (currentAnimation != null || !animationQueue.isEmpty()) {
                    kltyton_bedrock_runtime$unavailableSequence = currentRawAnimation;
                } else if (kltyton_bedrock_runtime$unavailableSequence == currentRawAnimation) {
                    Queue<QueuedAnimation> restored = model.getAnimationProcessor().buildAnimationQueue(animatable, currentRawAnimation);
                    if (restored != null) {
                        remaining.addAll(restored);
                    }
                }
            }
            if (active != null || !remaining.isEmpty()
                    || kltyton_bedrock_runtime$unavailableSequence != currentRawAnimation) {
                kltyton_bedrock_runtime$unavailableSequence = null;
            }
            currentAnimation = active;
            animationQueue = remaining;
        }
        kltyton_bedrock_runtime$animationResources = new WeakReference<>(resources);
    }

    @Unique
    private QueuedAnimation kltyton_bedrock_runtime$rebindStage(GeoModel<T> model, QueuedAnimation stage) {
        if (stage == null) {
            return null;
        }
        String name = ((IPSQueuedAnimation) (Object) stage).kltyton_bedrock_runtime$getStageName();
        if (name == null) {
            return stage;
        }
        Queue<QueuedAnimation> resolved = model.getAnimationProcessor().buildAnimationQueue(animatable,
                RawAnimation.begin().then(name, stage.loopType()));
        QueuedAnimation replacement = resolved == null ? null : resolved.peek();
        if (replacement == null) {
            Constants.LOG.warn("Animation stage '{}' is unavailable after resource reload for {}",
                    name, animatable.getClass().getName());
            return null;
        }
        return replacement.animation() == stage.animation() ? stage : replacement;
    }

    @Inject(method = "process", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$ensureParticleHandler(CallbackInfo ci) {
        kltyton_bedrock_runtime$wrapParticleHandler();
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$wrapCustomParticleHandler(AnimationController.ParticleKeyframeHandler<T> handler,
                                                         CallbackInfoReturnable<AnimationController<T>> cir) {
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
            target = "Lsoftware/bernie/geckolib/loading/math/MathParser;setVariable(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"))
    private void kltyton_bedrock_runtime$recordTransitionTime(CallbackInfo ci) {
        ActorAnimationContext.recordAnimationTime(animatable, 0.0F);
    }

    @Inject(method = "processCurrentAnimation", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/loading/math/MathParser;setVariable(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"))
    private void kltyton_bedrock_runtime$recordAnimationTime(double adjustedTick, double seekTime, boolean crashWhenCantFindBone,
            CallbackInfo ci) {
        ActorAnimationContext.recordAnimationTime(animatable, (float) (adjustedTick / 20.0));
    }

    @Inject(method = "getCurrentKeyFrameLocation", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$recordKeyframeLerp(List<Keyframe<MathValue>> frames, double ageInTicks,
            CallbackInfoReturnable<KeyframeLocation<Keyframe<MathValue>>> cir) {
        Keyframe<MathValue> selected = cir.getReturnValue().keyframe();
        double totalFrameTime = 0.0;
        for (Keyframe<MathValue> frame : frames) {
            totalFrameTime += frame.length();
            if (frame == selected) break;
        }
        ActorAnimationContext.recordKeyframeLerp(animatable, frames, ageInTicks, totalFrameTime, selected);
    }

    @Inject(method = "resetEventKeyFrames", at = @At("HEAD"))
    private void removeEmitters(CallbackInfo ci) {
        GeckoLibHelper.removeEmittersWhenAnimationChange(animationState, (AnimationController<?>) (Object) this,
                animatable.getAnimatableInstanceCache(), animatable);
    }
}
