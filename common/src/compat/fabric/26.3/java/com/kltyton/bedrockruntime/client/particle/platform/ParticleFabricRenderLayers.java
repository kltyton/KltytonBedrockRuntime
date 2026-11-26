package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers;
import com.kltyton.bedrockruntime.mixin.client.particle.RenderPipelinesInvoker;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.Optional;

/** Builds and installs the eight 26.3 particle layers (with OIT sets) on Fabric. */
public final class ParticleFabricRenderLayers {
    private ParticleFabricRenderLayers() {
    }

    public static void install() {
        RenderPipeline terrain = RenderPipeline.builder(RenderPipelines.ANIMATE_SPRITE_SNIPPET)
                .withFragmentShader(BedrockParticles.asResource("core/bedrock_terrain_animation"))
                .withLocation(BedrockParticles.asResource("pipeline/bedrock_terrain_animation"))
                .withBindGroupLayout(BindGroupLayouts.CURRENT_SPRITE_NEXT_SPRITE)
                .build();
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(terrain);
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
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(opaque);

        RenderPipeline alpha = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/alpha_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_alpha_test"))
                .withColorTargetState(opaqueColor)
                .build();
        SingleQuadParticle.Layer particleAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, alpha);
        SingleQuadParticle.Layer terrainAlpha = new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, alpha);
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(alpha);

        RenderPipeline additive = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/additive_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .build();
        OitPipelineSet additiveOit = buildOitSet("additive_particle",
                RenderPipeline.builder(RenderPipelines.OIT_PARTICLE_SNIPPET)
                        .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                        .withCull(false)
                        .withShaderDefine("OIT_ADDITIVE")
                        .buildSnippet());
        SingleQuadParticle.Layer particleAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, additive, additiveOit);
        SingleQuadParticle.Layer terrainAdd = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, additive, additiveOit);
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(additive);
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(additiveOit);

        RenderPipeline blend = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/blend_particle"))
                .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .build();
        OitPipelineSet blendOit = buildOitSet("blend_particle",
                RenderPipeline.builder(RenderPipelines.OIT_PARTICLE_SNIPPET)
                        .withFragmentShader(BedrockParticles.asResource("core/particle_no_discard"))
                        .withCull(false)
                        .buildSnippet());
        SingleQuadParticle.Layer particleBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, blend, blendOit);
        SingleQuadParticle.Layer terrainBlend = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_BLOCKS, blend, blendOit);
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(blend);
        RenderPipelinesInvoker.kltyton_bedrock_runtime$register(blendOit);

        ParticleRenderLayers.install(particleAdd, particleBlend, particleOpaque, particleAlpha,
                terrainAdd, terrainBlend, terrainOpaque, terrainAlpha);
    }

    private static OitPipelineSet buildOitSet(String suffix, RenderPipeline.Snippet baseSnippet) {
        RenderPipeline depthBounds = RenderPipeline.builder(baseSnippet, RenderPipelines.OIT_DEPTH_BOUNDS_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/oit_depth_bounds_" + suffix))
                .build();
        RenderPipeline transmittance = RenderPipeline.builder(baseSnippet, RenderPipelines.OIT_TRANSMITTANCE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/oit_transmittance_" + suffix))
                .build();
        RenderPipeline accumulate = RenderPipeline.builder(baseSnippet, RenderPipelines.OIT_ACCUMULATE_SNIPPET)
                .withLocation(BedrockParticles.asResource("pipeline/oit_accumulate_" + suffix))
                .build();
        return new OitPipelineSet(depthBounds, transmittance, accumulate);
    }
}
