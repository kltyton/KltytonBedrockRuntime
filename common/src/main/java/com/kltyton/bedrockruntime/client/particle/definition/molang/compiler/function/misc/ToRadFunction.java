package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Converts the input value to radians
 */
public final class ToRadFunction extends MathFunction {
    private final MathValue value;

    public ToRadFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.to_rad";
    }

    @Override
    public double compute(MolangInstance instance) {
        return Math.toRadians(this.value.get(instance));
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
