package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Evaluates the first log argument and returns its numeric value. */
public final class LogQuery extends MathFunction {
    private final MathValue[] arguments;

    public LogQuery(MathValue... arguments) {
        super(arguments);
        if (arguments.length != 1) throw new IllegalArgumentException("query.log requires one argument");
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        return arguments[0].get(instance);
    }

    @Override public String getName() { return "query.log"; }
    @Override public int getMinArgs() { return 1; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) {
        return values[0].isMutable();
    }
}
