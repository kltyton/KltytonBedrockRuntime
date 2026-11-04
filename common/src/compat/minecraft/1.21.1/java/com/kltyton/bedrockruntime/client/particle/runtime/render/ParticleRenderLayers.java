package com.kltyton.bedrockruntime.client.particle.runtime.render;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;

/**
 * Runtime holder for the particle layers used by Bedrock materials.
 *
 * <p>1.21.1 only renders the render types present in {@code ParticleEngine.RENDER_ORDER}, so the
 * default layer set maps onto the vanilla groups. A loader integration may install custom groups
 * through {@link #install}; they are then preferred by {@link #forMaterial}.</p>
 */
public final class ParticleRenderLayers {
    private static ParticleRenderType particleAdd = BedrockParticleRenderTypes.PARTICLE_ADD;
    private static ParticleRenderType particleBlend = BedrockParticleRenderTypes.PARTICLE_BLEND;
    private static ParticleRenderType particleOpaque = BedrockParticleRenderTypes.PARTICLE_OPAQUE;
    private static ParticleRenderType particleAlpha = BedrockParticleRenderTypes.PARTICLE_ALPHA;
    private static ParticleRenderType terrainParticleAdd = BedrockParticleRenderTypes.TERRAIN_ADD;
    private static ParticleRenderType terrainParticleBlend = BedrockParticleRenderTypes.TERRAIN_BLEND;
    private static ParticleRenderType terrainParticleOpaque = BedrockParticleRenderTypes.TERRAIN_OPAQUE;
    private static ParticleRenderType terrainParticleAlpha = BedrockParticleRenderTypes.TERRAIN_ALPHA;
    private static boolean installed;

    private ParticleRenderLayers() {
    }

    public static void install(ParticleRenderType particleAdd,
                               ParticleRenderType particleBlend,
                               ParticleRenderType particleOpaque,
                               ParticleRenderType particleAlpha,
                               ParticleRenderType terrainParticleAdd,
                               ParticleRenderType terrainParticleBlend,
                               ParticleRenderType terrainParticleOpaque,
                               ParticleRenderType terrainParticleAlpha) {
        if (installed && ParticleRenderLayers.particleAdd != particleAdd) {
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
        installed = true;
    }

    public static @Nullable ParticleRenderType forMaterial(DescriptionMaterial material, boolean terrainAtlas) {
        if (material == DescriptionMaterial.TERRAIN_SHEET) return ParticleRenderType.TERRAIN_SHEET;
        if (material == DescriptionMaterial.particles_opaque || material == DescriptionMaterial.PARTICLE_SHEET_OPAQUE) {
            return terrainAtlas ? terrainParticleOpaque() : particleOpaque();
        }
        if (material == DescriptionMaterial.particles_add) {
            return terrainAtlas ? terrainParticleAdd() : particleAdd();
        }
        if (material == DescriptionMaterial.particles_blend || material == DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT) {
            return terrainAtlas ? terrainParticleBlend() : particleBlend();
        }
        if (material == DescriptionMaterial.particles_alpha || material == DescriptionMaterial.PARTICLE_SHEET_LIT) {
            return terrainAtlas ? terrainParticleAlpha() : particleAlpha();
        }
        if (material == DescriptionMaterial.CUSTOM) {
            return terrainAtlas ? ParticleRenderType.TERRAIN_SHEET : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
        return null;
    }

    public static ParticleRenderType particleAdd() {
        return require(particleAdd, "particleAdd");
    }

    public static ParticleRenderType particleBlend() {
        return require(particleBlend, "particleBlend");
    }

    public static ParticleRenderType particleOpaque() {
        return require(particleOpaque, "particleOpaque");
    }

    public static ParticleRenderType particleAlpha() {
        return require(particleAlpha, "particleAlpha");
    }

    public static ParticleRenderType terrainParticleAdd() {
        return require(terrainParticleAdd, "terrainParticleAdd");
    }

    public static ParticleRenderType terrainParticleBlend() {
        return require(terrainParticleBlend, "terrainParticleBlend");
    }

    public static ParticleRenderType terrainParticleOpaque() {
        return require(terrainParticleOpaque, "terrainParticleOpaque");
    }

    public static ParticleRenderType terrainParticleAlpha() {
        return require(terrainParticleAlpha, "terrainParticleAlpha");
    }

    private static ParticleRenderType require(ParticleRenderType layer, String name) {
        if (layer == null) {
            throw new IllegalStateException("Particle render layer is not installed: " + name);
        }
        return layer;
    }
}
