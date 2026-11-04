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
 * Returns the magnitude of the first input with the sign of the second input.
 */
public final class CopySignFunction extends MathFunction {
    private final MathValue magnitude;
    private final MathValue sign;

    public CopySignFunction(MathValue... values) {
        super(values);

        this.magnitude = values[0];
        this.sign = values[1];
    }

    @Override
    public String getName() {
        return "math.copy_sign";
    }

    @Override
    public double compute(MolangInstance instance) {
        float magnitude = (float) this.magnitude.get(instance);
        float sign = (float) this.sign.get(instance);
        return Math.copySign(magnitude, sign);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.magnitude, this.sign};
    }
}
