package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.client.particle.ParticleEngine;

public interface IPSParticleEngine {
    void kltyton_bedrock_runtime$bindSprites();

    static IPSParticleEngine of(ParticleEngine engine) {
        return (IPSParticleEngine) engine;
    }
}
