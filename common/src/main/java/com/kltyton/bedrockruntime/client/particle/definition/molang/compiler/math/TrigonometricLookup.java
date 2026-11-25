package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math;

import com.kltyton.bedrockruntime.math.BedrockTrigonometry;

/** Bedrock's single-precision trigonometry for Molang. */
public final class TrigonometricLookup {
    private TrigonometricLookup() {}

    public static float sin(float radians) {
        return BedrockTrigonometry.sin(radians);
    }

    public static float cos(float radians) {
        return BedrockTrigonometry.cos(radians);
    }
}
