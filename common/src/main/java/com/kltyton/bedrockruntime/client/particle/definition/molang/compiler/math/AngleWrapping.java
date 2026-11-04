package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math;

/** Wraps degree values using a single-precision shifted remainder. */
public final class AngleWrapping {
    private AngleWrapping() {}

    public static float degrees(float value) {
        float shifted = (value + 180.0F) % 360.0F;
        if (shifted < 0.0F) shifted += 360.0F;
        return shifted - 180.0F;
    }
}
