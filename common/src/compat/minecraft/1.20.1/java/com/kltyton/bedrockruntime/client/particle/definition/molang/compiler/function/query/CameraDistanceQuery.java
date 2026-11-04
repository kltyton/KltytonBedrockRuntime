package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.util.Mth;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class CameraDistanceQuery extends MathFunction {
    public enum Kind { RANGE_LERP, LOD_INDEX }

    private final Kind kind;
    private final MathValue distance;
    private final MathValue[] arguments;

    public CameraDistanceQuery(Kind kind, MathValue distance, MathValue... arguments) {
        super(arguments);
        if (kind == Kind.RANGE_LERP && arguments.length != 2) {
            throw new IllegalArgumentException("query.camera_distance_range_lerp requires two distances");
        }
        this.kind = kind;
        this.distance = distance;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        float currentDistance = (float) distance.get(instance);
        if (kind == Kind.RANGE_LERP) {
            float start = (float) arguments[0].get(instance);
            float end = (float) arguments[1].get(instance);
            return start == end ? 0.0 : Mth.clamp((currentDistance - start) / (end - start), 0.0F, 1.0F);
        }
        for (int i = 0; i < arguments.length; i++) {
            if (currentDistance < (float) arguments[i].get(instance)) return i;
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
