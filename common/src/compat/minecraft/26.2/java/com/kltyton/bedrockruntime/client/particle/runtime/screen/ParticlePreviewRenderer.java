package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class ParticlePreviewRenderer extends PictureInPictureRenderer<ParticlePreviewRenderState> {
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("BedrockParticles editor preview");
    private final StagedVertexBuffer vertices = new StagedVertexBuffer(() -> "BedrockParticles editor particles", 65536);

    @Override
    public Class<ParticlePreviewRenderState> getRenderStateClass() {
        return ParticlePreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(ParticlePreviewRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        RenderSystem.setProjectionMatrix(projection.getBuffer(state.projection()),
                state.perspective() ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);
        RenderSystem.getModelViewStack().set(state.view());
        Map<RenderType, List<PreviewBlockQuad>> blockGroups = new LinkedHashMap<>();
        for (PreviewBlockQuad quad : state.blocks()) {
            blockGroups.computeIfAbsent(quad.quad().materialInfo().itemRenderType(), ignored -> new ArrayList<>()).add(quad);
        }
        List<RenderType> blockTypes = new ArrayList<>(blockGroups.keySet());
        blockTypes.sort(Comparator.comparing(RenderType::hasBlending));
        Map<RenderType, StagedVertexBuffer.Draw> blockDraws = new LinkedHashMap<>();
        for (RenderType type : blockTypes) {
            StagedVertexBuffer.Draw draw = vertices.appendDraw(type.format(), type.primitiveTopology());
            var builder = vertices.getVertexBuilder(draw);
            for (PreviewBlockQuad quad : blockGroups.get(type)) {
                builder.putBlockBakedQuad(quad.x(), quad.y(), quad.z(), quad.quad(), quad.instance());
            }
            blockDraws.put(type, draw);
        }
        Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> draws = new LinkedHashMap<>();
        List<SingleQuadParticle.Layer> layers = new ArrayList<>(state.particles().layers());
        layers.sort(Comparator.comparing(SingleQuadParticle.Layer::translucent)
                .thenComparing(layer -> layer.textureAtlasLocation().toString())
                .thenComparingInt(layer -> layer.pipeline().getSortKey()));
        for (SingleQuadParticle.Layer layer : layers) {
            StagedVertexBuffer.Draw draw = vertices.appendDraw(DefaultVertexFormat.PARTICLE, PrimitiveTopology.QUADS);
            state.particles().buildLayer(layer, vertices.getVertexBuilder(draw));
            draws.put(layer, draw);
        }
        vertices.upload();
        try {
            for (Map.Entry<RenderType, StagedVertexBuffer.Draw> entry : blockDraws.entrySet()) {
                StagedVertexBuffer.ExecuteInfo draw = vertices.getExecuteInfo(entry.getValue());
                if (draw != null) entry.getKey().prepare().drawFromBuffer(draw);
            }
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(state.view());
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Bedrock particle preview",
                    Objects.requireNonNull(RenderSystem.outputColorTextureOverride), Optional.empty(),
                    Objects.requireNonNull(RenderSystem.outputDepthTextureOverride), OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.levelLightmap(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                for (Map.Entry<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> entry : draws.entrySet()) {
                    StagedVertexBuffer.ExecuteInfo draw = vertices.getExecuteInfo(entry.getValue());
                    if (draw == null) continue;
                    SingleQuadParticle.Layer layer = entry.getKey();
                    AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(layer.textureAtlasLocation());
                    pass.setPipeline(layer.pipeline());
                    pass.setVertexBuffer(0, draw.vertexBuffer().slice());
                    pass.setIndexBuffer(draw.indexBuffer(), draw.indexType());
                    pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
                    pass.drawIndexed(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), 0);
                }
            }
        } finally {
            vertices.endFrame();
        }
        state.nativeParticles().submit(collector, state.camera());
        ((com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleSubmissionAccess)
                state.particles()).kltyton_bedrock_runtime$submitModelGeometry(collector);
    }

    @Override
    protected String getTextureLabel() {
        return "Bedrock particle preview";
    }

    @Override
    public void close() {
        super.close();
        projection.close();
        vertices.close();
    }
}
