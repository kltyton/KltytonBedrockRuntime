package com.kltyton.bedrockruntime.particle.platform;

/** Common/server-side particle wiring. Must not reference client-only types. */
public interface ParticlePlatform {
    void initializeCommon();
}
