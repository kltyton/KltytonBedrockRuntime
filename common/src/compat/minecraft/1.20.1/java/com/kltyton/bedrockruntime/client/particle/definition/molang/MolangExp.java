package com.kltyton.bedrockruntime.client.particle.definition.molang;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Constant;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.EvaluationScope;

import java.util.Map;

public class MolangExp {
    public static final MolangExp EMPTY = empty();
    public static final Codec<MolangExp> CODEC = Codec.either(Codec.STRING, Codec.either(Codec.FLOAT, Codec.BOOL)).xmap(
            value -> value.map(MolangExp::new, scalar -> scalar.map(
                    number -> new MolangExp(Float.toString(number)),
                    bool -> new MolangExp(bool ? "1.0" : "0.0"))),
            expression -> Either.left(expression.getExpStr()));
    protected final String expStr;
    protected MathValue variable;
    protected boolean immutable;

    private static MolangExp empty() {
        MolangExp empty = new MolangExp("");
        empty.variable = new Constant(0.0);
        return empty;
    }

    public MolangExp(String expStr) {
        this.expStr = expStr;
    }

    public MolangExp(String key, double value) {
        this.expStr = (key.startsWith("v.") ? "variable." + key.substring(2) : key) + "=" + value;
    }

    public MolangExp(Map<String, String> exps) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, String> entry : exps.entrySet()) {
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            if (++i < exps.size()) {
                builder.append(';');
            }
        }
        this.expStr = builder.toString();
    }

    public MolangExp(MathValue variable) {
        this.expStr = variable.toString();
        this.variable = variable;
    }

    public String getExpStr() {
        return expStr;
    }

    public void compile(MolangParser parser) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            this.variable = parser.compileMolang(expStr);
            if (immutable) {
                variable.markImmutable();
            }
        }
    }

    public float calculate(MolangInstance instance) {
        if (!initialized()) return 0.0F;
        return (float) MathValue.normalize(variable.get(instance));
    }

    public float calculate(MolangInstance instance, float currentValue) {
        EvaluationScope.pushThis(instance, currentValue);
        try {
            return calculate(instance);
        } finally {
            EvaluationScope.popThis();
        }
    }

    public MathValue getVariable() {
        return variable;
    }

    public void markImmutable() {
        this.immutable = true;
        if (variable != null) {
            variable.markImmutable();
        }
    }

    public boolean initialized() {
        return variable != null;
    }

    @Override
    public String toString() {
        return "MolangExp{" + expStr + "}";
    }
}
