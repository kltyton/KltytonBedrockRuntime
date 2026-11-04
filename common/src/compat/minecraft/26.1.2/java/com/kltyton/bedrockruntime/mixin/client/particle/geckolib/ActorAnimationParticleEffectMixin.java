package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carries source particle-effect fields through GeckoLib's keyframe bake. */
@Pseudo
@Mixin(targets = "com.geckolib.loading.definition.animation.ActorAnimationParticleEffect", remap = false)
public abstract class ActorAnimationParticleEffectMixin {
    @Shadow public abstract @Nullable String preEffectScript();
    @Shadow public abstract @Nullable Boolean bindToActor();

    @Inject(method = "bake", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$retainParticleFields(double timestamp, CallbackInfoReturnable<ParticleKeyframeData> cir) {
        IPSParticleKeyframeData.of(cir.getReturnValue()).kltyton_bedrock_runtime$setSource(preEffectScript(), bindToActor());
    }
}
