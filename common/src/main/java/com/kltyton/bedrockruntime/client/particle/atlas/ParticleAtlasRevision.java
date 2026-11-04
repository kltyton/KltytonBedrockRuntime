package com.kltyton.bedrockruntime.client.particle.atlas;

/** Tracks atlas uploads so existing particles can replace stale sprite coordinates. */
public final class ParticleAtlasRevision {
    private static volatile long current;

    private ParticleAtlasRevision() {}

    public static long current() {
        return current;
    }

    public static void stitched() {
        current++;
    }
}
