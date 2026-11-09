package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.kltyton.bedrockruntime.client.particle.access.IPSQueuedAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animation.AnimationProcessor;

/** Keeps the original lookup key even when a model resolves an alias to a differently named clip. */
@Mixin(value = AnimationProcessor.QueuedAnimation.class, remap = false)
abstract class QueuedAnimationMixin implements IPSQueuedAnimation {
    @Unique
    private String kltyton_bedrock_runtime$stageName;

    @Override
    public String kltyton_bedrock_runtime$getStageName() {
        return kltyton_bedrock_runtime$stageName;
    }

    @Override
    public void kltyton_bedrock_runtime$setStageName(String name) {
        kltyton_bedrock_runtime$stageName = name;
    }
}
