package com.kltyton.bedrockruntime.mixin.client.particle;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public interface ParticleEnginePreviewAccessor {
    @Invoker("updateCount")
    void kltyton_bedrock_runtime$updateGroupCount(ParticleGroup group, int delta);

    @Accessor("textureAtlas")
    TextureAtlas kltyton_bedrock_runtime$getTextureAtlas();

    @Mutable
    @Accessor("textureAtlas")
    void kltyton_bedrock_runtime$setTextureAtlas(TextureAtlas atlas);

    @Accessor("providers")
    Map<ResourceLocation, ParticleProvider<?>> kltyton_bedrock_runtime$getProviders();

    @Accessor("spriteSets")
    Map<ResourceLocation, Object> kltyton_bedrock_runtime$getSpriteSets();

    @Accessor("particles")
    Map<ParticleRenderType, Queue<Particle>> kltyton_bedrock_runtime$getParticles();

    @Accessor("particlesToAdd")
    Queue<Particle> kltyton_bedrock_runtime$getParticlesToAdd();

    @Accessor("trackedParticleCounts")
    Object2IntOpenHashMap<ParticleGroup> kltyton_bedrock_runtime$getTrackedParticleCounts();

    @Accessor("trackingEmitters")
    Queue<TrackingEmitter> kltyton_bedrock_runtime$getTrackingEmitters();
}
