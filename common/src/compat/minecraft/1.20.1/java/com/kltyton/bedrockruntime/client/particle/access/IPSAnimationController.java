package com.kltyton.bedrockruntime.client.particle.access;

import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Collection;
import java.util.List;

public interface IPSAnimationController {
    List<GeoBone> kltyton_bedrock_runtime$getBonesWhichHasLocators();

    void kltyton_bedrock_runtime$setBonesWhichHasLocators(Collection<CoreGeoBone> registeredBones);

    static IPSAnimationController of(AnimationController<?> controller) {
        return (IPSAnimationController) controller;
    }
}
