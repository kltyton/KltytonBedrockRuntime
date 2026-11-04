package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.Optional;

/** Builds and registers the eight 26.2 particle layers on NeoForge. */
public final class ParticleNeoForgeRenderLayers {
    private ParticleNeoForgeRenderLayers() {
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        RenderPipeline terrain = RenderPipeline.builder(RenderPipelines.ANIMATE_SPRITE_SNIPPET)
                .withFragmentShader(BedrockParticles.asResource("core/bedrock_terrain_animation"))
                .withLocation(BedrockParticles.asResource("pipeline/bedrock_terrain_animation"))
                .withBindGroupLayout(BindGroupLayouts.CURRENT_SPRITE_NEXT_SPRITE)
                .build();
        event.registerPipeline(terrain);
        ParticleRenderPipelines.installBedrockTerrainAnimation(terrain);

        ColorTargetState opaqueColor = new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM,
                ColorTargetState.WRITE_COLOR);
        RenderPipeline opaque = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/opaque_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(opaqueColor)
                .build();
        SingleQuadParticle.Layer particleOpaque = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, opaque);
        SingleQuadParticle.Layer terrainOpaque = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, opaque);
        event.registerPipeline(opaque);

        RenderPipeline alpha = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/alpha_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_alpha_test"))
                .withColorTargetState(opaqueColor)
                .build();
        SingleQuadParticle.Layer particleAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, alpha);
        SingleQuadParticle.Layer terrainAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, alpha);
        event.registerPipeline(alpha);

        RenderPipeline additive = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/additive_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, additive);
        SingleQuadParticle.Layer terrainAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, additive);
        event.registerPipeline(additive);

        RenderPipeline blend = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/blend_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, blend);
        SingleQuadParticle.Layer terrainBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, blend);
        event.registerPipeline(blend);

        ParticleRenderLayers.install(particleAdd, particleBlend, particleOpaque, particleAlpha,
                terrainAdd, terrainBlend, terrainOpaque, terrainAlpha);
    }
}
