package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.world.level.Level;

public interface IParticleComponent extends IComponent {
    default void update(IMolangParticleInstance instance) {}

    default void apply(IMolangParticleInstance instance) {}

    default boolean requireUpdate() {
        return false;
    }

    default void initialize(Level level) {}

    default void initialize(Level level, boolean bedrockBlockNames) {
        initialize(level);
    }
}
