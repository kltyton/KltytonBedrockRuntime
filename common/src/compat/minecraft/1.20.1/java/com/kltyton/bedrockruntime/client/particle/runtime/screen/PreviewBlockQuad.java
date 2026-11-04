package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.client.renderer.block.model.BakedQuad;

/** Immutable world-space block quad used by the screen particle preview. */
public record PreviewBlockQuad(float x, float y, float z, BakedQuad quad, int color, int light) {
}
