package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public interface ParticleEngineQueryAccessor {
    @Accessor("particles")
    Map<ParticleRenderType, ParticleGroup<?>> kltyton_bedrock_runtime$getParticles();

    @Accessor("particlesToAdd")
    Queue<Particle> kltyton_bedrock_runtime$getParticlesToAdd();
}