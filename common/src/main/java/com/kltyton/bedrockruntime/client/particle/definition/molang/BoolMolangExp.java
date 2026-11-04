package com.kltyton.bedrockruntime.client.particle.definition.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

public class BoolMolangExp extends MolangExp {
    public static final BoolMolangExp TRUE = new BoolMolangExp(true, "");
    public static final BoolMolangExp FALSE = new BoolMolangExp(false, "");
    public static final Codec<BoolMolangExp> CODEC = Codec.either(Codec.either(Codec.FLOAT, Codec.BOOL), Codec.STRING).xmap(
            either -> either.map(value -> value.map(f -> new BoolMolangExp(f != 0.0F, ""),
                    b -> new BoolMolangExp(b, "")), e -> new BoolMolangExp(true, e)),
            molang -> molang.expStr.isEmpty() ? Either.left(Either.right(molang.constant)) : Either.right(molang.expStr)
    );
    private final boolean constant;

    public BoolMolangExp(boolean constant, @Nullable String expression) {
        super(expression == null ? "" : expression);
        this.constant = constant;
    }

    public boolean getConstant() {
        return constant;
    }

    public boolean get(MolangInstance instance) {
        if (!initialized()) return false;
        return variable == null ? constant : MathValue.normalize(variable.get(instance)) != 0.0;
    }

    @Override
    public float calculate(MolangInstance instance) {
        return get(instance) ? 1.0F : 0.0F;
    }

    @Override
    public boolean initialized() {
        return constant || variable != null;
    }

    @Override
    public String toString() {
        return "BoolMolangExp{" + (expStr.isEmpty() ? constant : expStr) + '}';
    }

    public static BoolMolangExp ofExpression(String expression) {
        return new BoolMolangExp(true, expression);
    }
}
