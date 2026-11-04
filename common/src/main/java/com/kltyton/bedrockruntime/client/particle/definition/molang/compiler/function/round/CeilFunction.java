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
 * Returns the smallest value that is greater than or equal to the input value and is equal to an integer
 */
public final class CeilFunction extends MathFunction {
    private final MathValue value;

    public CeilFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.ceil";
    }

    @Override
    public double compute(MolangInstance instance) {
        return (float) StrictMath.ceil((float) this.value.get(instance));
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
