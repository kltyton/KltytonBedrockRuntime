package com.kltyton.bedrockruntime.client.particle.access;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Collection;
import java.util.List;

public interface IPSAnimationController {
    List<GeoBone> kltyton_bedrock_runtime$getBonesWhichHasLocators();

    void kltyton_bedrock_runtime$setBonesWhichHasLocators(Collection<GeoBone> registeredBones);

    void kltyton_bedrock_runtime$clearLocatorBones();

    static IPSAnimationController of(AnimationController<?> controller) {
        return (IPSAnimationController) controller;
    }
}
