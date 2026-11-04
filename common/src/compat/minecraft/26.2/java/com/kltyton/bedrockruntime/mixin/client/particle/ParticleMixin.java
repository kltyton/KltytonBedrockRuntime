package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.resources.Identifier;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.access.ParticleAdmission;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleMixin implements ParticleAdmission,
        com.kltyton.bedrockruntime.client.particle.access.ParticlePositionAccess {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void setLocationFromBoundingbox();

    /** Synchronizes the native position without rebuilding a caller-defined collision box. */
    @Override
    public void kltyton_bedrock_runtime$syncPositionFromBoundingBox() {
        setLocationFromBoundingbox();
    }

    @Unique
    private boolean kltyton_bedrock_runtime$admitted;
    @Unique
    private boolean kltyton_bedrock_runtime$released;
    @Unique private @Nullable MolangParticleManager kltyton_bedrock_runtime$owner;
    @Unique private @Nullable ParticleEmitter kltyton_bedrock_runtime$emitter;
    @Unique private @Nullable Identifier kltyton_bedrock_runtime$effect;
    @Unique private @Nullable IMolangParticleInstance kltyton_bedrock_runtime$instance;
    @Unique private boolean kltyton_bedrock_runtime$plainPreview;

    @Override
    public void kltyton_bedrock_runtime$admitted(MolangParticleManager owner) {
        if (kltyton_bedrock_runtime$admitted || kltyton_bedrock_runtime$released || !((Particle) (Object) this).isAlive()) return;
        if (this instanceof IMolangParticleInstance instance && instance.getEmitter() != null) {
            kltyton_bedrock_runtime$admitted = true;
            kltyton_bedrock_runtime$owner = owner;
            kltyton_bedrock_runtime$emitter = instance.getEmitter();
            kltyton_bedrock_runtime$effect = instance.getPreset().effect.description.identifier();
            kltyton_bedrock_runtime$instance = instance;
            owner.kltyton_bedrock_runtime$particleAdded(kltyton_bedrock_runtime$effect, instance);
            kltyton_bedrock_runtime$emitter.onAdded();
        } else if (owner.isIndependentParticles()) {
            kltyton_bedrock_runtime$admitted = true;
            kltyton_bedrock_runtime$owner = owner;
            kltyton_bedrock_runtime$plainPreview = true;
            owner.kltyton_bedrock_runtime$plainParticleAdded();
        }
    }

    @Override
    public void kltyton_bedrock_runtime$released() {
        if (kltyton_bedrock_runtime$released) return;
        kltyton_bedrock_runtime$released = true;
        if (kltyton_bedrock_runtime$admitted) {
            if (kltyton_bedrock_runtime$plainPreview) {
                kltyton_bedrock_runtime$owner.kltyton_bedrock_runtime$plainParticleRemoved();
            } else {
                kltyton_bedrock_runtime$owner.kltyton_bedrock_runtime$particleRemoved(kltyton_bedrock_runtime$effect, kltyton_bedrock_runtime$instance);
                kltyton_bedrock_runtime$emitter.onRemoved();
            }
        }
    }

    @Override
    public boolean kltyton_bedrock_runtime$isAdmitted() {
        return kltyton_bedrock_runtime$admitted && !kltyton_bedrock_runtime$released;
    }

    @Override
    public boolean kltyton_bedrock_runtime$isReleased() {
        return kltyton_bedrock_runtime$released;
    }

    @Override
    public @Nullable MolangParticleManager kltyton_bedrock_runtime$owner() {
        return kltyton_bedrock_runtime$owner;
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$countRemoval(CallbackInfo ci) {
        kltyton_bedrock_runtime$released();
    }
}
