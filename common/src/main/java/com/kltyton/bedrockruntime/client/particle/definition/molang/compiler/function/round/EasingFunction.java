package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.math.BedrockEasing;

/** Evaluates the single-precision easing functions available to Molang expressions. */
public final class EasingFunction extends MathFunction {
    public enum Family {
        SINE("sine"), QUAD("quad"), CUBIC("cubic"), QUART("quart"), QUINT("quint"),
        EXPO("expo"), CIRC("circ"), BACK("back"), ELASTIC("elastic"), BOUNCE("bounce");

        private final String name;
        Family(String name) { this.name = name; }
    }

    public enum Direction {
        IN("in"), OUT("out"), IN_OUT("in_out");

        private final String name;
        Direction(String name) { this.name = name; }
    }

    private final Family family;
    private final Direction direction;
    private final BedrockEasing curve;
    private final MathValue start;
    private final MathValue end;
    private final MathValue progress;

    public EasingFunction(Family family, Direction direction, MathValue... values) {
        super(values);
        this.family = family;
        this.direction = direction;
        this.curve = family == null || direction == null ? null : BedrockEasing.command(direction.name + "_" + family.name);
        this.start = values[0];
        this.end = values[1];
        this.progress = values[2];
    }

    public static String name(Family family, Direction direction) {
        return "math.ease_" + direction.name + "_" + family.name;
    }

    @Override
    public String getName() {
        return family == null || direction == null ? "math.ease_*" : name(family, direction);
    }

    @Override
    public double compute(MolangInstance instance) {
        return curve.interpolate((float) start.get(instance), (float) end.get(instance), (float) progress.get(instance));
    }

    @Override public int getMinArgs() { return 3; }
    @Override public MathValue[] getArgs() { return new MathValue[]{start, end, progress}; }
}
