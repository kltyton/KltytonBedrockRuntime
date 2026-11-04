package com.kltyton.bedrockruntime.client.particle.definition.molang;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;

import java.util.List;

public record FloatMolangExp3(FloatMolangExp exp1, FloatMolangExp exp2, FloatMolangExp exp3) {
    public static final FloatMolangExp3 ZERO = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 X = new FloatMolangExp3(FloatMolangExp.ONE, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Y = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ONE, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Z = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ONE);
    public static final Codec<FloatMolangExp3> CODEC = CodecCompat.list(FloatMolangExp.CODEC, 3, 3).xmap(
            exps -> new FloatMolangExp3(exps.get(0), exps.get(1), exps.get(2)),
            exp3 -> List.of(exp3.exp1, exp3.exp2, exp3.exp3)
    );
    public static final Codec<FloatMolangExp3> ZERO_COMPONENT_CODEC = CodecCompat.list(
            FloatMolangExp.CODEC.orElse((java.util.function.Consumer<String>) error ->
                    BedrockParticles.LOGGER.warn("Invalid particle vector component: {}", error), FloatMolangExp.ZERO), 3, 3
    ).xmap(exps -> new FloatMolangExp3(exps.get(0), exps.get(1), exps.get(2)),
            exp3 -> List.of(exp3.exp1, exp3.exp2, exp3.exp3));

    public float[] calculate(MolangInstance instance) {
        return new float[]{exp1.calculate(instance), exp2.calculate(instance), exp3.calculate(instance)};
    }

    public float[] calculate(MolangInstance instance, float x, float y, float z) {
        return new float[]{exp1.calculate(instance, x), exp2.calculate(instance, y), exp3.calculate(instance, z)};
    }

    public void markImmutable() {
        exp1.markImmutable();
        exp2.markImmutable();
        exp3.markImmutable();
    }

    @Override
    public String toString() {
        return "FloatMolangExp3{" +
                "exp1=" + exp1 +
                ", exp2=" + exp2 +
                ", exp3=" + exp3 +
                '}';
    }
}
