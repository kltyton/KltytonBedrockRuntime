package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.generic;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class MinAngleFunction extends MathFunction {
    private final MathValue value;

    public MinAngleFunction(MathValue... values) {
        super(values);
        value = values[0];
    }

    @Override
    public String getName() {
        return "math.min_angle";
    }

    @Override
    public double compute(MolangInstance instance) {
        return com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math.AngleWrapping.degrees((float) value.get(instance));
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{value};
    }
}
