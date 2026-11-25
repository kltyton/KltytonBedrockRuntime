package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Evaluates Bedrock query.get_root_locator_offset(locator, axis) used by vanilla armor offset animations. */
public final class RootLocatorOffsetQuery extends MathFunction {
    private final MathValue locator;
    private final MathValue axis;

    public RootLocatorOffsetQuery(MathValue... values) {
        super(values);
        this.locator = values[0];
        this.axis = values[1];
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] { locator, axis };
    }

    @Override
    public double compute(MolangInstance instance) {
        int index = (int) Math.floor(axis.get(instance));
        if (index < 0 || index > 2) return 0.0;
        return 0.0;
    }

    @Override
    public String getName() {
        return "query.get_root_locator_offset";
    }
}
