package com.kltyton.bedrockruntime.client.particle.runtime.render;

import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * 1.20.1 particle render-type holder. The client bootstrap installs the shader suppliers and
 * platform flags; material and ordering policy stays in the runtime.
 */
public final class ParticleRenderLayers {
    private static Supplier<ShaderInstance> particleNoDiscardShader = () -> null;
    private static Supplier<ShaderInstance> particleAlphaShader = () -> null;
    private static boolean irisLoaded;

    private ParticleRenderLayers() {
    }

    public static void installShaders(Supplier<ShaderInstance> noDiscard,
                                      Supplier<ShaderInstance> alpha,
                                      boolean irisLoaded) {
        particleNoDiscardShader = java.util.Objects.requireNonNull(noDiscard);
        particleAlphaShader = java.util.Objects.requireNonNull(alpha);
        ParticleRenderLayers.irisLoaded = irisLoaded;
    }

    public static @Nullable ShaderInstance particleNoDiscardShader() {
        return particleNoDiscardShader.get();
    }

    public static @Nullable ShaderInstance particleAlphaShader() {
        return particleAlphaShader.get();
    }

    public static boolean irisLoaded() {
        return irisLoaded;
    }

    public static ParticleRenderType particleAdd() {
        return BedrockParticleRenderTypes.PARTICLE_ADD;
    }

    public static ParticleRenderType particleBlend() {
        return BedrockParticleRenderTypes.PARTICLE_BLEND;
    }

    public static ParticleRenderType particleOpaque() {
        return BedrockParticleRenderTypes.PARTICLE_OPAQUE;
    }

    public static ParticleRenderType particleAlpha() {
        return BedrockParticleRenderTypes.PARTICLE_ALPHA;
    }

    public static ParticleRenderType terrainParticleAdd() {
        return BedrockParticleRenderTypes.TERRAIN_ADD;
    }

    public static ParticleRenderType terrainParticleBlend() {
        return BedrockParticleRenderTypes.TERRAIN_BLEND;
    }

    public static ParticleRenderType terrainParticleOpaque() {
        return BedrockParticleRenderTypes.TERRAIN_OPAQUE;
    }

    public static ParticleRenderType terrainParticleAlpha() {
        return BedrockParticleRenderTypes.TERRAIN_ALPHA;
    }

    public static ParticleRenderType forMaterial(DescriptionMaterial material, boolean terrain) {
        return BedrockParticleRenderTypes.forMaterial(material, terrain);
    }

    public static List<ParticleRenderType> renderOrder(List<ParticleRenderType> original) {
        return BedrockParticleRenderTypes.renderOrder(original);
    }

    public static BedrockParticleRenderTypes.DrawScope enterDraw(ParticleRenderType type) {
        return BedrockParticleRenderTypes.enterDraw(type);
    }
}
