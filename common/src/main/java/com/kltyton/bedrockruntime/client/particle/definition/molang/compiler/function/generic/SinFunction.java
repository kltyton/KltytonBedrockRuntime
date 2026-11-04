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
 * Returns the sine of the input value angle, with the input angle converted to radians
 */
public final class SinFunction extends MathFunction {
    private final MathValue value;

    public SinFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.sin";
    }

    @Override
    public double compute(MolangInstance instance) {
        float radians = (float) this.value.get(instance) * 0.017453292F;
        return (float) StrictMath.sin(radians);
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value};
    }
}
