package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangValueEquality;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Implements the numeric range and variadic equality queries. */
public final class ComparisonQuery extends MathFunction {
    public enum Kind { ALL, ANY, IN_RANGE, APPROX_EQ }
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();

    private final Kind kind;
    private final MathValue[] arguments;

    public ComparisonQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.IN_RANGE ? arguments.length != 3
                : arguments.length < (kind == Kind.APPROX_EQ ? 2 : 3)) {
            return contentError("invalid argument count");
        }
        if (kind == Kind.APPROX_EQ) {
            float first = (float) arguments[0].get(instance);
            for (int i = 1; i < arguments.length; i++) {
                if (first != (float) arguments[i].get(instance)) return 0.0;
            }
            return 1.0;
        }
        Object first = MathValue.resolveRaw(arguments[0], instance);
        if (kind == Kind.IN_RANGE) {
            if (!(first instanceof Number value)) return contentError("requires numeric arguments");
            Object minimum = MathValue.resolveRaw(arguments[1], instance);
            if (!(minimum instanceof Number low)) return contentError("requires numeric arguments");
            float number = value.floatValue();
            if (number < low.floatValue()) return 0.0;
            Object maximum = MathValue.resolveRaw(arguments[2], instance);
            if (!(maximum instanceof Number high)) return contentError("requires numeric arguments");
            return number <= high.floatValue() ? 1.0 : 0.0;
        }
        first = MolangValueEquality.snapshot(first);
        for (int i = 1; i < arguments.length; i++) {
            Object candidate = MathValue.resolveRaw(arguments[i], instance);
            boolean equal = MolangValueEquality.same(first, candidate);
            if (kind == Kind.ALL && !equal) return 0.0;
            if (kind == Kind.ANY && equal) return 1.0;
        }
        return kind == Kind.ALL ? 1.0 : 0.0;
    }

    private double contentError(String reason) {
        String message = getName() + ": " + reason;
        if (REPORTED_ERRORS.add(message)) BedrockParticles.LOGGER.warn("Molang {}", message);
        return 0.0;
    }

    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public String getName() {
        return switch (kind) {
            case ALL -> "query.all";
            case ANY -> "query.any";
            case IN_RANGE -> "query.in_range";
            case APPROX_EQ -> "query.approx_eq";
        };
    }
}
