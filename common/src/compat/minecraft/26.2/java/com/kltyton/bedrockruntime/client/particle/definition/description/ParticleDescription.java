package com.kltyton.bedrockruntime.client.particle.definition.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

public record ParticleDescription(Identifier identifier, DescriptionParameters parameters, ParticleType<?> type) {
    public static final Codec<ParticleDescription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("identifier").forGetter(ParticleDescription::identifier),
            DescriptionParameters.CODEC.optionalFieldOf("basic_render_parameters", DescriptionParameters.EMPTY).forGetter(ParticleDescription::parameters),
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().lenientOptionalFieldOf("type", BedrockParticles.MOLANG).forGetter(ParticleDescription::type)
    ).apply(instance, ParticleDescription::new));

    public ParticleDescription(Identifier identifier, DescriptionParameters parameters) {
        this(identifier, parameters, BedrockParticles.MOLANG);
    }
}
