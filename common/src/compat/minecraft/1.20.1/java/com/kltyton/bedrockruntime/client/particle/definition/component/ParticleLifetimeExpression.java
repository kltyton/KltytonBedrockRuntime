package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// Standard lifetime component. These expressions control the lifetime of the particle.
///
/// @param expirationExpression This expression makes the particle expire when true (non-zero)<p>
///                             The float/expr is evaluated once per particle<p>
///                             Evaluated every frame<p>
/// @param maxLifetime          Alternate way to express lifetime<p>
///                             Particle will expire after this much time<p>
///                             Evaluated once
public record ParticleLifetimeExpression(FloatMolangExp expirationExpression, FloatMolangExp maxLifetime) implements IParticleComponent {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("particle_lifetime_expression");
    public static final Codec<ParticleLifetimeExpression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.optionalFieldOf("expiration_expression", FloatMolangExp.ZERO).forGetter(ParticleLifetimeExpression::expirationExpression),
            FloatMolangExp.CODEC.optionalFieldOf("max_lifetime", FloatMolangExp.ofConstant(1_000_000.0F)).forGetter(ParticleLifetimeExpression::maxLifetime)
    ).apply(instance, ParticleLifetimeExpression::new));

    @Override
    public Codec<ParticleLifetimeExpression> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(expirationExpression, maxLifetime);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        if (expirationExpression.calculate(instance) != 0.0F) {
            instance.self().remove();
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.setLifetimeSeconds(maxLifetime.calculate(instance));
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleLifetimeExpression{" +
                "expirationExpression=" + expirationExpression +
                ", maxLifetime=" + maxLifetime +
                '}';
    }
}
