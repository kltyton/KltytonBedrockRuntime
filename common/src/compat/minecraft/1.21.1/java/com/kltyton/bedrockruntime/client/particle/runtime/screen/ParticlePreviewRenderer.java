package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;

public final class ParticlePreviewRenderer {
    private ParticlePreviewRenderer() {
    }

    public static void render(ParticlePreviewRenderState state) {
        render(state, new PoseStack());
    }

    public static void render(ParticlePreviewRenderState state, PoseStack poseStack) {
        RenderSystem.setProjectionMatrix(state.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().pushMatrix();
        try {
            RenderSystem.getModelViewStack().set(state.view());
            RenderSystem.applyModelViewMatrix();
            List<PreviewBlockQuad> blocks = state.blocks();
            if (!blocks.isEmpty()) {
                MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                for (PreviewBlockQuad block : blocks) {
                    poseStack.pushPose();
                    poseStack.translate(block.x(), block.y(), block.z());
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.state(), poseStack, buffers,
                            0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
                buffers.endBatch();
            }
            state.owner().renderParticles(state.previewCamera(), state.partialTick(), null);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static String getTextureLabel() {
        return "particle editor preview";
    }
}
