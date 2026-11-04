package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.limit;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the first input value if is larger than the second input value and less than the third input value; or else returns the nearest of the second two input values
 */
public final class ClampFunction extends MathFunction {
    private final MathValue value;
    private final MathValue min;
    private final MathValue max;

    public ClampFunction(MathValue... values) {
        super(values);

        this.value = values[0];
        this.min = values[1];
        this.max = values[2];
    }

    @Override
    public String getName() {
        return "math.clamp";
    }

    @Override
    public double compute(MolangInstance instance) {
        float value = (float) this.value.get(instance);
        float min = (float) this.min.get(instance);
        float max = (float) this.max.get(instance);
        return max < value ? max : value <= min ? min : value;
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value, this.min, this.max};
    }
}
