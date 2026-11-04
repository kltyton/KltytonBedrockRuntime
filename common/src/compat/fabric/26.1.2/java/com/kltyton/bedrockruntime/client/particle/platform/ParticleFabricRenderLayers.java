package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.Optional;

/** Builds and installs the eight 26.1.2 particle layers on Fabric. */
public final class ParticleFabricRenderLayers {
    private ParticleFabricRenderLayers() {
    }

    public static void install() {
        RenderPipeline terrain = RenderPipeline.builder(RenderPipelines.ANIMATE_SPRITE_SNIPPET)
                .withFragmentShader(BedrockParticles.asResource("core/bedrock_terrain_animation"))
                .withLocation(BedrockParticles.asResource("pipeline/bedrock_terrain_animation"))
                .withSampler("CurrentSprite")
                .withSampler("NextSprite")
                .build();
        com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker.kltyton_bedrock_runtime$register(terrain);
        ParticleRenderPipelines.installBedrockTerrainAnimation(terrain);

        ColorTargetState opaqueColor = new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_COLOR);
        RenderPipeline opaque = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/opaque_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(opaqueColor)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleOpaque = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, opaque);
        SingleQuadParticle.Layer terrainOpaque = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, opaque);
        com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker.kltyton_bedrock_runtime$register(opaque);

        RenderPipeline alpha = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/alpha_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_alpha_test"))
                .withColorTargetState(opaqueColor)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, alpha);
        SingleQuadParticle.Layer terrainAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, alpha);
        com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker.kltyton_bedrock_runtime$register(alpha);

        RenderPipeline additive = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/additive_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withCull(true)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, additive);
        SingleQuadParticle.Layer terrainAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, additive);
        com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker.kltyton_bedrock_runtime$register(additive);

        RenderPipeline blend = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/blend_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(true)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        SingleQuadParticle.Layer particleBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, blend);
        SingleQuadParticle.Layer terrainBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, blend);
        com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker.kltyton_bedrock_runtime$register(blend);

        ParticleRenderLayers.install(particleAdd, particleBlend, particleOpaque, particleAlpha,
                terrainAdd, terrainBlend, terrainOpaque, terrainAlpha);
    }
}
