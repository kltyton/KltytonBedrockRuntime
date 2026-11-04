package com.kltyton.bedrockruntime.client.particle.runtime.timing;

import net.minecraft.world.level.Level;

/** Monotonic update intervals and time conversions for particle simulation. */
public final class ParticleSimulationClock {
    private static final long STEP_THRESHOLD_NANOS = 101_000_000L;
    private static final long CLAMPED_STEP_NANOS = 100_000_000L;
    private long previousNanos;
    private long deltaNanos;
    private boolean initialized;

    public long sample(boolean paused) {
        long now = System.nanoTime();
        if (!initialized || paused) {
            initialized = true;
            previousNanos = now;
            deltaNanos = 0L;
        } else {
            long elapsed = now - previousNanos;
            previousNanos = now;
            deltaNanos = elapsed < STEP_THRESHOLD_NANOS ? elapsed : CLAMPED_STEP_NANOS;
        }
        return deltaNanos;
    }

    public void pause() {
        initialized = true;
        previousNanos = System.nanoTime();
        deltaNanos = 0L;
    }

    public long deltaNanos() {
        return deltaNanos;
    }

    public void reset() {
        initialized = false;
        deltaNanos = 0L;
    }

    public static float seconds(long nanos) {
        return (float) nanos / 1_000_000_000.0F;
    }

    public static long nanos(float seconds) {
        return (long) (seconds * 1_000_000_000.0F);
    }

    public static float interpolateSeconds(long previous, long current, float partialTick) {
        return (float) ((previous + (current - previous) * (double) partialTick) / 1_000_000_000.0);
    }

    public static float nominalTickSeconds(Level level) {
        return 1.0F / level.tickRateManager().tickrate();
    }
}
