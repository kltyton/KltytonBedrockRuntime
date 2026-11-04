package com.kltyton.bedrockruntime.particle.platform;

import java.util.Objects;

/** Holds the common/server particle platform selected by the loader common entrypoint. */
public final class ParticlePlatforms {
    private static volatile ParticlePlatform platform;

    private ParticlePlatforms() {
    }

    public static void installCommon(ParticlePlatform adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ParticlePlatform current = platform;
        if (current != null && current.getClass() != adapter.getClass()) {
            throw new IllegalStateException("Particle common platform already installed: "
                    + current.getClass().getName());
        }
        platform = adapter;
    }

    public static ParticlePlatform requireCommon() {
        ParticlePlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Particle common platform has not been installed");
        }
        return current;
    }
}
