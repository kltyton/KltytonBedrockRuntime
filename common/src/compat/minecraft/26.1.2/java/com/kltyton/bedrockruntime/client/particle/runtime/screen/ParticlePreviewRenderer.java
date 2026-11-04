package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticlePreviewRenderer extends PictureInPictureRenderer<ParticlePreviewRenderState> {
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("BedrockParticles editor preview");

    public ParticlePreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<ParticlePreviewRenderState> getRenderStateClass() {
        return ParticlePreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(ParticlePreviewRenderState state, PoseStack poseStack) {
        RenderSystem.setProjectionMatrix(projection.getBuffer(state.projection()),
                state.perspective() ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.set(state.view());
            Map<RenderType, List<PreviewBlockQuad>> blockGroups = new LinkedHashMap<>();
            for (PreviewBlockQuad quad : state.blocks()) {
                blockGroups.computeIfAbsent(quad.quad().materialInfo().itemRenderType(), ignored -> new ArrayList<>()).add(quad);
            }
            List<RenderType> blockTypes = new ArrayList<>(blockGroups.keySet());
            blockTypes.sort(Comparator.comparing(RenderType::hasBlending));
            for (RenderType type : blockTypes) {
                var vertices = bufferSource.getBuffer(type);
                for (PreviewBlockQuad quad : blockGroups.get(type)) {
                    vertices.putBlockBakedQuad(quad.x(), quad.y(), quad.z(), quad.quad(), quad.instance());
                }
                bufferSource.endBatch(type);
            }
            var dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
            var submissions = dispatcher.getSubmitNodeStorage();
            state.particles().submit(submissions, state.camera());
            state.nativeParticles().submit(submissions, state.camera());
            dispatcher.renderAllFeatures();
        } finally {
            modelView.popMatrix();
        }
    }

    @Override
    protected String getTextureLabel() {
        return "particle editor preview";
    }

    @Override
    public void close() {
        super.close();
        projection.close();
    }
}
