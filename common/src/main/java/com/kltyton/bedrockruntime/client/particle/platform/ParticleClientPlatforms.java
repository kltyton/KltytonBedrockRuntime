package com.kltyton.bedrockruntime.client.particle.platform;

import java.util.Objects;

/** Holds the client-only particle platform selected by the loader client entrypoint. */
public final class ParticleClientPlatforms {
    private static volatile ParticleClientPlatform platform;

    private ParticleClientPlatforms() {
    }

    public static void install(ParticleClientPlatform adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ParticleClientPlatform current = platform;
        if (current != null && current.getClass() != adapter.getClass()) {
            throw new IllegalStateException("Particle client platform already installed: "
                    + current.getClass().getName());
        }
        platform = adapter;
    }

    public static ParticleClientPlatform require() {
        ParticleClientPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Particle client platform has not been installed");
        }
        return current;
    }
}
