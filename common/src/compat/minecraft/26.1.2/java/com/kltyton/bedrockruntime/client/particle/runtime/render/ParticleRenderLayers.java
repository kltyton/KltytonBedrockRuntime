package com.kltyton.bedrockruntime.client.particle.runtime.render;

import net.minecraft.client.particle.SingleQuadParticle;

/**
 * Runtime holder for the particle layers created by the client bootstrap.
 */
public final class ParticleRenderLayers {
    private static SingleQuadParticle.Layer particleAdd;
    private static SingleQuadParticle.Layer particleBlend;
    private static SingleQuadParticle.Layer particleOpaque;
    private static SingleQuadParticle.Layer particleAlpha;
    private static SingleQuadParticle.Layer terrainParticleAdd;
    private static SingleQuadParticle.Layer terrainParticleBlend;
    private static SingleQuadParticle.Layer terrainParticleOpaque;
    private static SingleQuadParticle.Layer terrainParticleAlpha;

    private ParticleRenderLayers() {
    }

    public static void install(SingleQuadParticle.Layer particleAdd,
                               SingleQuadParticle.Layer particleBlend,
                               SingleQuadParticle.Layer particleOpaque,
                               SingleQuadParticle.Layer particleAlpha,
                               SingleQuadParticle.Layer terrainParticleAdd,
                               SingleQuadParticle.Layer terrainParticleBlend,
                               SingleQuadParticle.Layer terrainParticleOpaque,
                               SingleQuadParticle.Layer terrainParticleAlpha) {
        if (ParticleRenderLayers.particleAdd != null
                && ParticleRenderLayers.particleAdd != particleAdd) {
            throw new IllegalStateException("Particle render layers already installed");
        }
        ParticleRenderLayers.particleAdd = particleAdd;
        ParticleRenderLayers.particleBlend = particleBlend;
        ParticleRenderLayers.particleOpaque = particleOpaque;
        ParticleRenderLayers.particleAlpha = particleAlpha;
        ParticleRenderLayers.terrainParticleAdd = terrainParticleAdd;
        ParticleRenderLayers.terrainParticleBlend = terrainParticleBlend;
        ParticleRenderLayers.terrainParticleOpaque = terrainParticleOpaque;
        ParticleRenderLayers.terrainParticleAlpha = terrainParticleAlpha;
    }

    public static SingleQuadParticle.Layer particleAdd() {
        return require(particleAdd, "particleAdd");
    }

    public static SingleQuadParticle.Layer particleBlend() {
        return require(particleBlend, "particleBlend");
    }

    public static SingleQuadParticle.Layer particleOpaque() {
        return require(particleOpaque, "particleOpaque");
    }

    public static SingleQuadParticle.Layer particleAlpha() {
        return require(particleAlpha, "particleAlpha");
    }

    public static SingleQuadParticle.Layer terrainParticleAdd() {
        return require(terrainParticleAdd, "terrainParticleAdd");
    }

    public static SingleQuadParticle.Layer terrainParticleBlend() {
        return require(terrainParticleBlend, "terrainParticleBlend");
    }

    public static SingleQuadParticle.Layer terrainParticleOpaque() {
        return require(terrainParticleOpaque, "terrainParticleOpaque");
    }

    public static SingleQuadParticle.Layer terrainParticleAlpha() {
        return require(terrainParticleAlpha, "terrainParticleAlpha");
    }

    private static SingleQuadParticle.Layer require(SingleQuadParticle.Layer layer, String name) {
        if (layer == null) {
            throw new IllegalStateException("Particle render layer is not installed: " + name);
        }
        return layer;
    }
}
