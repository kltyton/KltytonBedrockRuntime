package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.round;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math.TrigonometricLookup;

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
    private final MathValue start;
    private final MathValue end;
    private final MathValue progress;

    public EasingFunction(Family family, Direction direction, MathValue... values) {
        super(values);
        this.family = family;
        this.direction = direction;
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
        float from = (float) start.get(instance);
        float to = (float) end.get(instance);
        float t = (float) progress.get(instance);
        float delta = to - from;
        return switch (family) {
            case QUAD -> polynomial(from, delta, t, 2);
            case CUBIC -> polynomial(from, delta, t, 3);
            case QUART -> polynomial(from, delta, t, 4);
            case QUINT -> polynomial(from, delta, t, 5);
            case SINE -> sine(from, delta, t);
            case EXPO -> exponential(from, delta, t);
            case CIRC -> circular(from, delta, t);
            case BACK -> back(from, delta, t);
            case BOUNCE -> bouncing(from, delta, t);
            case ELASTIC -> elastic(from, delta, t);
        };
    }

    private float polynomial(float from, float delta, float t, int degree) {
        if (direction == Direction.IN) return scaledPower(delta, t, degree) + from;
        if (direction == Direction.OUT) {
            if (degree == 2) return from - (t - 2.0F) * delta * t;
            float value = power(t - 1.0F, degree);
            return (degree & 1) == 0 ? from - (value - 1.0F) * delta
                    : (value + 1.0F) * delta + from;
        }
        float doubled = t + t;
        if (doubled < 1.0F) return from + scaledPower(delta * 0.5F, doubled, degree);
        if (degree == 2) {
            return from + delta * -0.5F * ((-2.0F + doubled - 1.0F) * (doubled - 1.0F) - 1.0F);
        }
        float value = power(doubled - 2.0F, degree);
        return from + ((degree & 1) == 0 ? delta * -0.5F * (value - 2.0F)
                : delta * 0.5F * (value + 2.0F));
    }

    private static float scaledPower(float scale, float value, int degree) {
        for (int i = 0; i < degree; i++) scale *= value;
        return scale;
    }

    private static float power(float value, int degree) {
        return scaledPower(value, value, degree - 1);
    }

    private float sine(float from, float delta, float t) {
        return switch (direction) {
            case IN -> (delta - TrigonometricLookup.cos(t * 1.5707964F) * delta) + from;
            case OUT -> delta * TrigonometricLookup.sin(t * 1.5707964F) + from;
            case IN_OUT -> (TrigonometricLookup.cos(t * 3.1415927F) - 1.0F) * (delta * -0.5F) + from;
        };
    }

    private float exponential(float from, float delta, float t) {
        if (direction == Direction.IN) return exp2((t - 1.0F) * 10.0F) * delta + from;
        if (direction == Direction.OUT) return (1.0F - exp2(t * -10.0F)) * delta + from;
        float doubled = t + t;
        float value = doubled < 1.0F ? exp2((doubled - 1.0F) * 10.0F)
                : 2.0F - exp2((doubled - 1.0F) * -10.0F);
        return from + value * delta * 0.5F;
    }

    private float circular(float from, float delta, float t) {
        if (direction == Direction.IN) return from - delta * (sqrt(1.0F - t * t) - 1.0F);
        if (direction == Direction.OUT) {
            float shifted = t - 1.0F;
            return from + delta * sqrt(1.0F - shifted * shifted);
        }
        float doubled = t + t;
        if (doubled < 1.0F) return from + delta * -0.5F * (-1.0F + sqrt(1.0F - doubled * doubled));
        float shifted = doubled - 2.0F;
        return from + delta * 0.5F * (1.0F + sqrt(1.0F - shifted * shifted));
    }

    private float back(float from, float delta, float t) {
        if (direction == Direction.IN) return (t * 2.70158F - 1.70158F) * delta * t * t + from;
        if (direction == Direction.OUT) {
            float shifted = t - 1.0F;
            return ((shifted * 2.70158F + 1.70158F) * shifted * shifted + 1.0F) * delta + from;
        }
        float doubled = t + t;
        float value;
        if (doubled >= 1.0F) {
            float shifted = doubled - 2.0F;
            value = (shifted * 3.5949094F + 2.5949094F) * shifted * shifted + 2.0F;
        } else {
            value = (doubled * 3.5949094F - 2.5949094F) * doubled * doubled;
        }
        return from + value * (delta * 0.5F);
    }

    private float bouncing(float from, float delta, float t) {
        if (direction == Direction.IN) return (delta - (bounce(1.0F - t) * delta + 0.0F)) + from;
        if (direction == Direction.OUT) return delta * bounce(t) + from;
        if (t < 0.5F) {
            return from + ((delta - (bounce(1.0F - (t + t)) * delta + 0.0F)) + 0.0F) * 0.5F;
        }
        return from + delta * 0.5F + (bounce(t + t - 1.0F) * delta + 0.0F) * 0.5F;
    }

    private static float bounce(float t) {
        if (t < 4.0F / 11.0F) return 7.5625F * t * t;
        if (t < 8.0F / 11.0F) {
            t -= 6.0F / 11.0F;
            return 7.5625F * t * t + 0.75F;
        }
        if (t < 10.0F / 11.0F) {
            t -= 9.0F / 11.0F;
            return 7.5625F * t * t + 0.9375F;
        }
        t -= 10.5F / 11.0F;
        return 7.5625F * t * t + 0.984375F;
    }

    private float elastic(float from, float delta, float t) {
        if (t == 0.0F) return from;
        if (t == 1.0F) return from + delta;
        if (direction == Direction.IN) {
            float shifted = t - 1.0F;
            float sine = TrigonometricLookup.sin((shifted - 0.075F) * 6.2831855F / 0.3F);
            return from - delta * exp2(10.0F * shifted) * sine;
        }
        if (direction == Direction.OUT) {
            float sine = TrigonometricLookup.sin((t - 0.075F) * 6.2831855F / 0.3F);
            return from + exp2(t * -10.0F) * delta * sine + delta;
        }
        float doubled = t + t;
        float shifted = doubled - 1.0F;
        float sine = TrigonometricLookup.sin((shifted - 0.075F) * 6.2831855F / 0.3F);
        return doubled >= 1.0F ? from + exp2(shifted * -10.0F) * delta * sine * 0.5F + delta
                : from + delta * exp2(shifted * 10.0F) * sine * -0.5F;
    }

    private static float exp2(float exponent) {
        return (float) StrictMath.pow(2.0, exponent);
    }

    private static float sqrt(float value) {
        return (float) StrictMath.sqrt(value);
    }

    @Override public int getMinArgs() { return 3; }
    @Override public MathValue[] getArgs() { return new MathValue[]{start, end, progress}; }
}
