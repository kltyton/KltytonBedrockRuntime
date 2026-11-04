package com.kltyton.bedrockruntime.client.particle.access;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

/** Tracks whether a particle is owned by a particle manager. */
public interface ParticleAdmission {
    void kltyton_bedrock_runtime$admitted(MolangParticleManager owner);
    void kltyton_bedrock_runtime$released();
    boolean kltyton_bedrock_runtime$isAdmitted();
    boolean kltyton_bedrock_runtime$isReleased();
    @Nullable MolangParticleManager kltyton_bedrock_runtime$owner();
}
