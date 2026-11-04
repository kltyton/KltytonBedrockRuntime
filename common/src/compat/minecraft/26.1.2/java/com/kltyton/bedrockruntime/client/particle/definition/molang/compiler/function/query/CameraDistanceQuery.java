package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class CameraDistanceQuery extends MathFunction {
    public enum Kind { RANGE_LERP, LOD_INDEX }

    private final Kind kind;
    private final MathValue distance;
    private final MathValue[] arguments;
    private static final java.util.concurrent.atomic.AtomicBoolean REPORTED_RANGE_ARGUMENTS = new java.util.concurrent.atomic.AtomicBoolean();

    public CameraDistanceQuery(Kind kind, MathValue distance, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.distance = distance;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.RANGE_LERP) {
            if (arguments.length != 2) {
                if (REPORTED_RANGE_ARGUMENTS.compareAndSet(false, true)) {
                    BedrockParticles.LOGGER.warn("query.camera_distance_range_lerp requires two distances");
                }
                return 0.0;
            }
            float start = (float) arguments[0].get(instance);
            float end = (float) arguments[1].get(instance);
            float minimum = start <= end ? start : end;
            float maximum = start <= end ? end : start;
            float currentDistance = (float) distance.get(instance);
            float fraction = 0.0F;
            if (minimum < currentDistance) {
                fraction = 1.0F;
                if (currentDistance < maximum) {
                    fraction = maximum - minimum >= Math.ulp(1.0F)
                            ? (currentDistance - minimum) / (maximum - minimum) : 0.0F;
                }
            }
            return end < start ? 1.0F - fraction : fraction;
        }
        for (int i = 0; i < arguments.length; i++) {
            float currentDistance = (float) distance.get(instance);
            if (currentDistance <= (float) arguments[i].get(instance)) return i;
        }
        return arguments.length;
    }

    @Override public String getName() {
        return kind == Kind.RANGE_LERP ? "query.camera_distance_range_lerp" : "query.lod_index";
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
