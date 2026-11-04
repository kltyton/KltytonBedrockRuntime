package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.molang.FrameTimeHistory;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;

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
    private final MathValue[] arguments;
    private final @Nullable MathValue previousFrames;

    public FrameTimeQuery(Kind kind, MathValue... values) {
        super(values);
        this.kind = kind;
        this.arguments = values.clone();
        this.previousFrames = arguments.length == 0 ? null : arguments[0];
    }

    @Override
    public double compute(MolangInstance instance) {
        if (ActorAccess.hasEmptyBaseRenderContext(instance)) return 0.0;
        float requested = previousFrames == null ? 0.0F : (float) previousFrames.get(instance);
        long direct = nativeLong(requested);
        long shifted = nativeLong(requested - 0x1.0p63F);
        long index = direct | (direct >> 63) & shifted;
        int previous = kind == Kind.LAST ? (int) Long.remainderUnsigned(index, 30)
                : Long.compareUnsigned(index, 29) > 0 ? 29 : (int) index;
        return FrameTimeHistory.query(kind.historyKind, previous);
    }

    private static long nativeLong(float value) {
        return Float.isNaN(value) || value >= 0x1.0p63F || value < -0x1.0p63F
                ? Long.MIN_VALUE : (long) value;
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
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
