package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;

import java.util.Objects;

/** Renders the preview state retained by the editor-free screen runtime. */
public final class ParticlePreviewRenderer {
    private ParticlePreviewRenderer() {
    }

    public static void render(ParticlePreviewRenderState state) {
        Objects.requireNonNull(state, "state");
        MolangParticleManager owner = state.owner();
        owner.renderParticles(state.lightTexture(), state.textureManager(), state.poseStack(),
                state.camera(), state.partialTick(), state.frustum(), state.layerPredicate());
    }
}
