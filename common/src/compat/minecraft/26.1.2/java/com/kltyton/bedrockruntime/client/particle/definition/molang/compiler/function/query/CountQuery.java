package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.List;

/** Counts arguments, expanding arrays by their element count. */
public final class CountQuery extends MathFunction {
    private final MathValue[] arguments;

    public CountQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override public double compute(MolangInstance instance) {
        int count = 0;
        for (MathValue argument : arguments) {
            Object value = argument.getRaw(instance);
            count += value instanceof List<?> list ? list.size() : 1;
        }
        return count;
    }
    @Override public String getName() { return "query.count"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
}
