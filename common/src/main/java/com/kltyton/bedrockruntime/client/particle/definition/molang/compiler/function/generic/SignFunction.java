package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.generic;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class SignFunction extends MathFunction {
    private final MathValue value;

    public SignFunction(MathValue... values) {
        super(values);
        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.sign";
    }

    @Override
    public double compute(MolangInstance instance) {
        return (float) value.get(instance) < 0.0F ? -1.0F : 1.0F;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {value};
    }
}
