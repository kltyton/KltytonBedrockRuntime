package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import net.minecraft.client.particle.Particle;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public class ParticleMixin {
    @Unique
    private boolean kltyton_bedrock_runtime$removalCounted = false;

    @Inject(method = "remove", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$countRemoval(CallbackInfo ci) {
        if (kltyton_bedrock_runtime$removalCounted) return;
        kltyton_bedrock_runtime$removalCounted = true;
        if (this instanceof IMolangParticleInstance instance && instance.getEmitter() != null) {
            MolangParticleEngine.INSTANCE.unregisterParticle(instance);
        }
    }
}
