package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import net.minecraft.world.level.block.state.BlockState;

/** A preview block instance captured at its world position. */
public record PreviewBlockQuad(float x, float y, float z, BlockState state) {
    public static PreviewBlockQuad snapshot(float x, float y, float z, BlockState state) {
        return new PreviewBlockQuad(x, y, z, state);
    }
}
