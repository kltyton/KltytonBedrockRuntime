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
public abstract class ParticleMixin implements com.kltyton.bedrockruntime.client.particle.access.ParticlePositionAccess {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void setLocationFromBoundingbox();

    /** Synchronizes the native position without rebuilding a caller-defined collision box. */
    @Override
    public void kltyton_bedrock_runtime$syncPositionFromBoundingBox() {
        setLocationFromBoundingbox();
    }

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
