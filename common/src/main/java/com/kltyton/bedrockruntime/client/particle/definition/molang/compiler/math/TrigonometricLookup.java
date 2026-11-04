package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.math;

/** Evaluates single-precision angles through a 16-bit sine table. */
public final class TrigonometricLookup {
    private static final float SCALE = 10430.378F;

    private TrigonometricLookup() {}

    public static float sin(float radians) {
        return lookup(radians * SCALE);
    }

    public static float cos(float radians) {
        return lookup(radians * SCALE + 16384.0F);
    }

    private static float lookup(float scaled) {
        // A masked native float-to-int overflow selects entry zero.
        int index = scaled >= -0x1.0p31F && scaled < 0x1.0p31F ? (int) scaled & 0xffff : 0;
        return SineTable.VALUES[index];
    }

    private static final class SineTable {
        private static final float[] VALUES = create();

        private static float[] create() {
            float[] values = new float[65536];
            for (int i = 0; i < values.length; i++) {
                float angle = (float) i / SCALE;
                values[i] = (float) StrictMath.sin(angle);
            }
            return values;
        }
    }
}
