package com.kltyton.bedrockruntime.client.particle.platform;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.Nullable;

/**
 * Shared holder for pipelines registered by the particle renderer. The runtime worker owns the
 * pipeline definitions; the texture mixins only read the selected terrain-animation pipeline.
 */
public final class ParticleRenderPipelines {
    private static volatile @Nullable RenderPipeline bedrockTerrainAnimation;

    private ParticleRenderPipelines() {
    }

    public static void installBedrockTerrainAnimation(RenderPipeline pipeline) {
        if (bedrockTerrainAnimation != null && bedrockTerrainAnimation != pipeline) {
            throw new IllegalStateException("Bedrock terrain animation pipeline already installed");
        }
        bedrockTerrainAnimation = pipeline;
    }

    public static @Nullable RenderPipeline bedrockTerrainAnimation() {
        return bedrockTerrainAnimation;
    }
}
