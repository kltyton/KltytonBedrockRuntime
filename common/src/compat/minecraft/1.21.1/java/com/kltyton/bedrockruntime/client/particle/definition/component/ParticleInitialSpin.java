package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified orientation and rotation rate.
///
/// @param rotation     Specifies the initial rotation in degrees. Evaluated once
/// @param rotationRate Specifies the spin rate in degrees/second. Evaluated once
public record ParticleInitialSpin(FloatMolangExp rotation, FloatMolangExp rotationRate) implements IParticleComponent {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.withDefaultNamespace("particle_initial_spin");
    public static final Codec<ParticleInitialSpin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.lenientOptionalFieldOf("rotation", FloatMolangExp.ZERO).forGetter(ParticleInitialSpin::rotation),
            FloatMolangExp.CODEC.lenientOptionalFieldOf("rotation_rate", FloatMolangExp.ZERO).forGetter(ParticleInitialSpin::rotationRate)
    ).apply(instance, ParticleInitialSpin::new));

    @Override
    public Codec<ParticleInitialSpin> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(rotation, rotationRate);
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.setZRot(rotation.calculate(instance) * Mth.DEG_TO_RAD);
        instance.setZRotD(rotationRate.calculate(instance) * instance.getVelocityTimeScale() * Mth.DEG_TO_RAD);
    }

    @Override
    public int order() {
        return -100;
    }
}
