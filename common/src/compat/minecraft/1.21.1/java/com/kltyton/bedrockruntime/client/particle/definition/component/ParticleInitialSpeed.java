package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified speed, using the direction specified by the emitter shape.
///
/// @param speed Evaluated once
public record ParticleInitialSpeed(Either<FloatMolangExp, FloatMolangExp3> speed) implements IParticleComponent {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.withDefaultNamespace("particle_initial_speed");
    public static final Codec<ParticleInitialSpeed> CODEC = Codec.either(FloatMolangExp.CODEC, FloatMolangExp3.CODEC).xmap(
            either -> either.map(f -> new ParticleInitialSpeed(Either.left(f)), l -> new ParticleInitialSpeed(Either.right(l))),
            ParticleInitialSpeed::speed
    );

    @Override
    public Codec<ParticleInitialSpeed> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return speed.map(List::of, exp3 -> List.of(exp3.exp1(), exp3.exp2(), exp3.exp3()));
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        speed.ifLeft(exp -> {
            var velocity = instance.getInitialSpeed();
            float x = exp.calculate(instance);
            float y = exp.calculate(instance);
            float z = exp.calculate(instance);
            float length = velocity.length();
            if (length < 1.0E-4F) {
                velocity.zero();
            } else {
                velocity.div(length).mul(x, y, z);
            }
        }).ifRight(exp3 -> instance.getInitialSpeed().set(exp3.calculate(instance)));
    }

    @Override
    public int order() {
        return -100;
    }

    @Override
    public String toString() {
        return "ParticleInitialSpeed{" +
                "speed=" + speed +
                '}';
    }
}
