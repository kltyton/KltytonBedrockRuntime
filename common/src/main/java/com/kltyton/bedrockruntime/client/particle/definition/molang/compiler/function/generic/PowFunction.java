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
 * Returns the input value raised to the power of the second input value
 */
public final class PowFunction extends MathFunction {
    private final MathValue value;
    private final MathValue power;

    public PowFunction(MathValue... values) {
        super(values);

        this.value = values[0];
        this.power = values[1];
    }

    @Override
    public String getName() {
        return "math.pow";
    }

    @Override
    public double compute(MolangInstance instance) {
        float value = (float) this.value.get(instance);
        float power = (float) this.power.get(instance);
        return (float) StrictMath.pow(value, power);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value, this.power};
    }
}
