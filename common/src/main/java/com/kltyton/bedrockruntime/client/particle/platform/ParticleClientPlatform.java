package com.kltyton.bedrockruntime.client.particle.platform;

/** Client-only particle wiring. Created only from a real client entrypoint. */
public interface ParticleClientPlatform {
    void initializeClient();
}
