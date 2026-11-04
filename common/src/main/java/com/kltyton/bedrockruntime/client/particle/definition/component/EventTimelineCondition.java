package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import java.util.function.Function;

/** Event timestamps are included at the start of an update interval and excluded at its end. */
record EventTimelineCondition(float seconds) implements Function<Integer, Boolean> {
    @Override
    public Boolean apply(Integer ticks) {
        return ticks > seconds * 20.0F;
    }

    long timestampNanos() {
        return ParticleSimulationClock.nanos(seconds);
    }

    boolean reached(float elapsedSeconds) {
        return seconds < elapsedSeconds;
    }
}
