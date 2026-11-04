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
 * Returns the closest value that is equal to the input value or closer to zero, and is equal to an integer
 */
public final class TruncateFunction extends MathFunction {
    private final MathValue value;

    public TruncateFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.trunc";
    }

    @Override
    public double compute(MolangInstance instance) {
        float value = (float) this.value.get(instance);
        return (float) (value < 0.0F ? StrictMath.ceil(value) : StrictMath.floor(value));
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
