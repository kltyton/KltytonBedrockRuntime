package com.kltyton.bedrockruntime.math;

import java.util.Locale;

/** Single-precision Bedrock curves for Molang and camera interpolation. */
public enum BedrockEasing {
    LINEAR(Family.LINEAR, Direction.DIRECT),
    SPRING(Family.SPRING, Direction.DIRECT),
    IN_QUAD(Family.QUAD, Direction.IN),
    OUT_QUAD(Family.QUAD, Direction.OUT),
    IN_OUT_QUAD(Family.QUAD, Direction.IN_OUT),
    IN_CUBIC(Family.CUBIC, Direction.IN),
    OUT_CUBIC(Family.CUBIC, Direction.OUT),
    IN_OUT_CUBIC(Family.CUBIC, Direction.IN_OUT),
    IN_QUART(Family.QUART, Direction.IN),
    OUT_QUART(Family.QUART, Direction.OUT),
    IN_OUT_QUART(Family.QUART, Direction.IN_OUT),
    IN_QUINT(Family.QUINT, Direction.IN),
    OUT_QUINT(Family.QUINT, Direction.OUT),
    IN_OUT_QUINT(Family.QUINT, Direction.IN_OUT),
    IN_SINE(Family.SINE, Direction.IN),
    OUT_SINE(Family.SINE, Direction.OUT),
    IN_OUT_SINE(Family.SINE, Direction.IN_OUT),
    IN_EXPO(Family.EXPO, Direction.IN),
    OUT_EXPO(Family.EXPO, Direction.OUT),
    IN_OUT_EXPO(Family.EXPO, Direction.IN_OUT),
    IN_CIRC(Family.CIRC, Direction.IN),
    OUT_CIRC(Family.CIRC, Direction.OUT),
    IN_OUT_CIRC(Family.CIRC, Direction.IN_OUT),
    IN_BOUNCE(Family.BOUNCE, Direction.IN),
    OUT_BOUNCE(Family.BOUNCE, Direction.OUT),
    IN_OUT_BOUNCE(Family.BOUNCE, Direction.IN_OUT),
    IN_BACK(Family.BACK, Direction.IN),
    OUT_BACK(Family.BACK, Direction.OUT),
    IN_OUT_BACK(Family.BACK, Direction.IN_OUT),
    IN_ELASTIC(Family.ELASTIC, Direction.IN),
    OUT_ELASTIC(Family.ELASTIC, Direction.OUT),
    IN_OUT_ELASTIC(Family.ELASTIC, Direction.IN_OUT);

    private enum Family { LINEAR, SPRING, QUAD, CUBIC, QUART, QUINT, SINE, EXPO, CIRC, BOUNCE, BACK, ELASTIC }

    private enum Direction { DIRECT, IN, OUT, IN_OUT }

    private final Family family;
    private final Direction direction;
    private final String commandName;
    private final String scriptName;

    BedrockEasing(Family family, Direction direction) {
        this.family = family;
        this.direction = direction;
        commandName = name().toLowerCase(Locale.ROOT);
        var script = new StringBuilder();
        for (String part : commandName.split("_")) {
            script.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        scriptName = script.toString();
    }

    public String commandName() {
        return commandName;
    }

    public static BedrockEasing command(String name) {
        for (var easing : values()) if (easing.commandName.equals(name)) return easing;
        throw new IllegalArgumentException("Unknown camera easing " + name);
    }

    public static BedrockEasing script(String name) {
        for (var easing : values()) if (easing.scriptName.equals(name)) return easing;
        throw new IllegalArgumentException("Unknown camera easing " + name);
    }

    public float progress(double time) {
        if (time <= 0) return 0;
        if (time >= 1) return 1;
        return interpolate(0, 1, (float) time);
    }

    public float interpolate(float from, float to, float t) {
        float delta = to - from;
        return switch (family) {
            case LINEAR -> from + delta * t;
            case SPRING -> spring(from, delta, t);
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

    private static float spring(float from, float delta, float time) {
        float t = Math.max(0.0F, Math.min(1.0F, time));
        float remaining = 1.0F - t;
        float cycles = 2.5F * t * t * t + 0.2F;
        float oscillation = BedrockTrigonometry.sin((3.1415927F * t) * cycles);
        float decay = (float) StrictMath.pow(remaining, 2.2F);
        float progress = (oscillation * decay + t) * (1.0F + remaining * 1.2F);
        return from + delta * progress;
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
            case IN -> (delta - BedrockTrigonometry.cos(t * 1.5707964F) * delta) + from;
            case OUT -> delta * BedrockTrigonometry.sin(t * 1.5707964F) + from;
            case IN_OUT -> (BedrockTrigonometry.cos(t * 3.1415927F) - 1.0F) * (delta * -0.5F) + from;
            case DIRECT -> throw new IllegalStateException("Sine easing requires an easing direction");
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
            float sine = BedrockTrigonometry.sin((shifted - 0.075F) * 6.2831855F / 0.3F);
            return from - delta * exp2(10.0F * shifted) * sine;
        }
        if (direction == Direction.OUT) {
            float sine = BedrockTrigonometry.sin((t - 0.075F) * 6.2831855F / 0.3F);
            return from + exp2(t * -10.0F) * delta * sine + delta;
        }
        float doubled = t + t;
        float shifted = doubled - 1.0F;
        float sine = BedrockTrigonometry.sin((shifted - 0.075F) * 6.2831855F / 0.3F);
        return doubled >= 1.0F ? from + exp2(shifted * -10.0F) * delta * sine * 0.5F + delta
                : from + delta * exp2(shifted * 10.0F) * sine * -0.5F;
    }

    private static float exp2(float exponent) {
        return (float) StrictMath.pow(2.0, exponent);
    }

    private static float sqrt(float value) {
        return (float) StrictMath.sqrt(value);
    }
}
