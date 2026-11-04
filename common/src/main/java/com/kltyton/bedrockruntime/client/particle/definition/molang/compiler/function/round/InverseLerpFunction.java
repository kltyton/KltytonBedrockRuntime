package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class InverseLerpFunction extends MathFunction {
    private final MathValue start;
    private final MathValue end;
    private final MathValue value;

    public InverseLerpFunction(MathValue... values) {
        super(values);
        this.start = values[0];
        this.end = values[1];
        this.value = values[2];
    }

    @Override
    public String getName() {
        return "math.inverse_lerp";
    }

    @Override
    public double compute(MolangInstance instance) {
        float start = (float) this.start.get(instance);
        float end = (float) this.end.get(instance);
        float value = (float) this.value.get(instance);
        return start == end ? 0.0 : (value - start) / (end - start);
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {start, end, value};
    }
}
