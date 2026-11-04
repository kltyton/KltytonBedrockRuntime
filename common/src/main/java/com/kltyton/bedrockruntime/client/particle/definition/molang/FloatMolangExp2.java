package com.kltyton.bedrockruntime.client.particle.definition.molang;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;

import java.util.List;

public record FloatMolangExp2(FloatMolangExp exp1, FloatMolangExp exp2) {
    public static final FloatMolangExp2 ZERO = new FloatMolangExp2(FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final Codec<FloatMolangExp2> CODEC = CodecCompat.list(FloatMolangExp.CODEC, 2, 2).xmap(
            exps -> new FloatMolangExp2(exps.get(0), exps.get(1)),
            exp2 -> List.of(exp2.exp1, exp2.exp2)
    );

    public float[] calculate(MolangInstance instance) {
        return new float[]{exp1.calculate(instance), exp2.calculate(instance)};
    }

    public void markImmutable() {
        exp1.markImmutable();
        exp2.markImmutable();
    }

    @Override
    public String toString() {
        return "FloatMolangExp2{" +
                "exp1=" + exp1 +
                ", exp2=" + exp2 +
                '}';
    }
}
