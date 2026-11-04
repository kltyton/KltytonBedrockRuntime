package com.kltyton.bedrockruntime.client.particle.model.platform;

import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelMolangParticle;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import com.kltyton.bedrockruntime.particle.registry.ModelParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;

/** 1.20.1 particle provider factory; the type itself lives in the common-safe registry holder. */
public final class ModelParticlePlatform {
    private ModelParticlePlatform() {
    }

    public static ParticleType<MolangParticleOption> modelParticleType() {
        return ModelParticleTypes.modelParticleType();
    }

    public static ModelMolangParticle createParticle(ParticleEmitter emitter, ParticlePreset preset,
                                                     ClientLevel level, double x, double y, double z) {
        return new ModelMolangParticle(preset, level, x, y, z, level.getRandom());
    }

    public static void registerParticleType(Registry<ParticleType<?>> registry) {
        ModelParticleTypes.registerParticleType(registry);
    }
}
