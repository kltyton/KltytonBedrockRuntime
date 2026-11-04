package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.core.particles.ParticleGroup;

/** A mutable emitter particle limit backed by the 1.21.1 vanilla group value. */
public final class MutableParticleGroup extends ParticleGroup {
    private int limit;

    public MutableParticleGroup(int limit) {
        super(limit);
        this.limit = limit;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public ParticleGroup asParticleLimit() {
        return this;
    }
}
