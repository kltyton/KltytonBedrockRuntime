package com.kltyton.bedrockruntime.particle.registry;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;

/** Common-safe declaration and registry entry for the model particle type. */
public final class ModelParticleTypes {
    public static final String ID_PATH = "model_particle";
    private static final ParticleType<MolangParticleOption> MODEL_PARTICLE = new ParticleType<>(false) {
        @Override
        public MapCodec<MolangParticleOption> codec() {
            return MolangParticleOption.CODEC;
        }

        @Override
        public StreamCodec<ByteBuf, MolangParticleOption> streamCodec() {
            return MolangParticleOption.STREAM_CODEC;
        }
    };

    private ModelParticleTypes() {
    }

    public static ParticleType<MolangParticleOption> modelParticleType() {
        return MODEL_PARTICLE;
    }

    public static void registerParticleType(Registry<ParticleType<?>> registry) {
        Registry.register(registry, BedrockParticles.asResource(ID_PATH), MODEL_PARTICLE);
    }
}
