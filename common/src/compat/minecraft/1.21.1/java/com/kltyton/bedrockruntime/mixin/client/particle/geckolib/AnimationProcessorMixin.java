package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.kltyton.bedrockruntime.client.particle.access.IPSAnimationController;
import com.kltyton.bedrockruntime.client.particle.access.IPSQueuedAnimation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Collection;

/** Supplies locator bones and records the lookup behind each named animation stage. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationProcessor", remap = false)
public abstract class AnimationProcessorMixin<T extends GeoAnimatable> {
    @Shadow
    public abstract Collection<GeoBone> getRegisteredBones();

    @Inject(method = "tickAnimation", at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/animation/AnimationController;process(Lsoftware/bernie/geckolib/model/GeoModel;Lsoftware/bernie/geckolib/animation/AnimationState;Ljava/util/Map;Ljava/util/Map;DZ)V"))
    private void fillLocators(CallbackInfo ci, @Local(name = "controller") AnimationController<T> controller) {
        IPSAnimationController.of(controller).kltyton_bedrock_runtime$setBonesWhichHasLocators(getRegisteredBones());
    }

    @WrapOperation(method = "buildAnimationQueue", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/model/GeoModel;getAnimation(Lsoftware/bernie/geckolib/animatable/GeoAnimatable;Ljava/lang/String;)Lsoftware/bernie/geckolib/animation/Animation;"))
    private Animation kltyton_bedrock_runtime$rememberStageLookup(GeoModel<T> model, T owner, String name,
            Operation<Animation> original, @Share("stageLookup") LocalRef<String> stageLookup) {
        Animation animation = original.call(model, owner, name);
        stageLookup.set(animation == null ? null : name);
        return animation;
    }

    @ModifyExpressionValue(method = "buildAnimationQueue", at = @At(value = "NEW",
            target = "Lsoftware/bernie/geckolib/animation/AnimationProcessor$QueuedAnimation;"))
    private QueuedAnimation kltyton_bedrock_runtime$rememberStage(QueuedAnimation queued,
            @Share("stageLookup") LocalRef<String> stageLookup) {
        ((IPSQueuedAnimation) (Object) queued).kltyton_bedrock_runtime$setStageName(stageLookup.get());
        stageLookup.set(null);
        return queued;
    }
}
