package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// When this component exists, particle will be tinted by local lighting conditions in-game.
public final class ParticleAppearanceLighting implements IParticleComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("particle_appearance_lighting");
    public static final ParticleAppearanceLighting INSTANCE = new ParticleAppearanceLighting();
    public static final Codec<ParticleAppearanceLighting> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(ParticleAppearanceLighting.INSTANCE)).codec();

    private ParticleAppearanceLighting() {}

    @Override
    public Codec<ParticleAppearanceLighting> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }
}
