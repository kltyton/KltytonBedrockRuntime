package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the <a href="https://en.wikipedia.org/wiki/Hermite_polynomials">Hermite</a>> basis <code>3t^2 - 2t^3</code> curve interpolation value based on the input value
 */
public final class HermiteBlendFunction extends MathFunction {
    private final MathValue valueA;

    public HermiteBlendFunction(MathValue... values) {
        super(values);

        this.valueA = values[0];
    }

    @Override
    public String getName() {
        return "math.hermite_blend";
    }

    @Override
    public double compute(MolangInstance instance) {
        float value = (float) this.valueA.get(instance);
        return 3.0F * value * value - (value + value) * value * value;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.valueA};
    }
}
