package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("particles")
    Map<ParticleRenderType, ParticleGroup<?>> kltyton_bedrock_runtime$getParticles();

    @Accessor("particlesToAdd")
    Queue<Particle> kltyton_bedrock_runtime$getParticlesToAdd();

    @Accessor("resourceManager")
    ParticleResources kltyton_bedrock_runtime$getParticleResources();
    /** Keeps independent preview limits synchronized with the native engine. */
    @Accessor("trackedParticleCounts")
    it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<net.minecraft.core.particles.ParticleLimit>
            kltyton_bedrock_runtime$getTrackedParticleCounts();
}
