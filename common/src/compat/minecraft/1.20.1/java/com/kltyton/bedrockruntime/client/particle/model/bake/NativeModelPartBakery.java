package com.kltyton.bedrockruntime.client.particle.model.bake;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelUvSpace;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

import java.util.List;

/** Uses native ModelPart rendering to retain its hierarchy, visibility, vertex units and normals. */
public final class NativeModelPartBakery {
    private NativeModelPartBakery() {
    }

    public static ModelParticleModel bake(String id, ModelPart root, String texture,
                                          ModelMaterial material, boolean doubleSided) {
        Adapter vertices = new Adapter();
        PoseStack poses = new PoseStack();
        poses.scale(-1.0F, -1.0F, 1.0F);
        root.render(poses, vertices, 0, OverlayTexture.NO_OVERLAY);
        return new ModelParticleModel(id, texture, List.of(new ModelParticlePart(texture, material,
                ModelUvSpace.NORMALIZED, vertices.capture.finish(), new Matrix4f(), doubleSided)),
                material, doubleSided);
    }

    private static final class Adapter implements VertexConsumer {
        private final ModelPartQuadCapture capture = new ModelPartQuadCapture();

        private int defaultColor = 0xFFFFFFFF;
        @Override
        public VertexConsumer vertex(double x, double y, double z) { capture.vertex((float) x, (float) y, (float) z, defaultColor); return this; }
        @Override
        public VertexConsumer color(int r, int g, int b, int a) { capture.color((a << 24) | (r << 16) | (g << 8) | b); return this; }
        @Override
        public VertexConsumer uv(float u, float v) { capture.uv(u, v); return this; }
        @Override
        public VertexConsumer overlayCoords(int u, int v) {  return this; }
        @Override
        public VertexConsumer uv2(int u, int v) { capture.light(u, v); return this; }
        @Override
        public VertexConsumer normal(float x, float y, float z) { capture.normal(x, y, z); return this; }
        @Override
        public void endVertex() { capture.endVertex(); }
        @Override
        public void defaultColor(int r, int g, int b, int a) { defaultColor = (a << 24) | (r << 16) | (g << 8) | b; }
        @Override
        public void unsetDefaultColor() { defaultColor = 0xFFFFFFFF; }
    }
}

