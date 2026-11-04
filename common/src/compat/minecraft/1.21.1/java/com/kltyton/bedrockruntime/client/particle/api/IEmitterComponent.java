package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

public interface IEmitterComponent extends IComponent {
    default void update(ParticleEmitter entity) {}

    default void apply(ParticleEmitter entity) {}

    default boolean requireUpdate() {
        return false;
    }
}
