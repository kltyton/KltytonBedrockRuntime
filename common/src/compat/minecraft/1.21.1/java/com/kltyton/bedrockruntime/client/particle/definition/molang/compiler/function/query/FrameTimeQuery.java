package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.molang.FrameTimeHistory;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class FrameTimeQuery extends MathFunction {
    public enum Kind {
        LAST(FrameTimeHistory.Kind.LAST), AVERAGE(FrameTimeHistory.Kind.AVERAGE),
        MINIMUM(FrameTimeHistory.Kind.MINIMUM), MAXIMUM(FrameTimeHistory.Kind.MAXIMUM);

        private final FrameTimeHistory.Kind historyKind;

        Kind(FrameTimeHistory.Kind historyKind) {
            this.historyKind = historyKind;
        }
    }

    private final Kind kind;
    private final @Nullable MathValue previousFrames;

    public FrameTimeQuery(Kind kind, MathValue... values) {
        super(values);
        if (values.length > 1) throw new IllegalArgumentException("Frame time query accepts at most one argument");
        this.kind = kind;
        this.previousFrames = values.length == 0 ? null : values[0];
    }

    @Override
    public double compute(MolangInstance instance) {
        int previous = previousFrames == null ? 0 : (int) previousFrames.get(instance);
        return FrameTimeHistory.query(kind.historyKind, previous);
    }

    @Override public String getName() {
        return switch (kind) {
            case LAST -> "query.last_frame_time";
            case AVERAGE -> "query.average_frame_time";
            case MINIMUM -> "query.minimum_frame_time";
            case MAXIMUM -> "query.maximum_frame_time";
        };
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return previousFrames == null ? new MathValue[0] : new MathValue[]{previousFrames}; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
