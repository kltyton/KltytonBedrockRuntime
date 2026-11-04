package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.core.particles.ParticleGroup;

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
