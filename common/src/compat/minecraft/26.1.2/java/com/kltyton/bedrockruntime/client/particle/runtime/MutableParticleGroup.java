package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.core.particles.ParticleLimit;

/** A mutable emitter particle limit backed by the 26.2 native limit value. */
public class MutableParticleGroup {
    private int limit;
    private ParticleLimit nativeLimit;

    public MutableParticleGroup(int limit) {
        setLimit(limit);
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (nativeLimit == null || this.limit != limit) {
            this.limit = limit;
            this.nativeLimit = new ParticleLimit(limit);
        }
    }

    public ParticleLimit asParticleLimit() {
        return nativeLimit;
    }
}
