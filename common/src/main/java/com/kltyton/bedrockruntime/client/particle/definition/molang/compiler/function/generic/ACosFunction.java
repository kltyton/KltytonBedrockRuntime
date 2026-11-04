package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.generic;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the arc-cosine of the input value, in degrees
 */
public final class ACosFunction extends MathFunction {
    private final MathValue value;

    public ACosFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.acos";
    }

    @Override
    public double compute(MolangInstance instance) {
        float value = (float) this.value.get(instance);
        if (Math.abs(value) <= 1.0005F) value = Math.max(-1.0F, Math.min(1.0F, value));
        return (float) StrictMath.acos(value) * 57.295776F;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{this.value};
    }
}
