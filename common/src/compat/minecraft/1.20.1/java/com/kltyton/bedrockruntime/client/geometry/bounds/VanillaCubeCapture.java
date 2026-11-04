package com.kltyton.bedrockruntime.client.geometry.bounds;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

final class VanillaCubeCapture {
    private VanillaCubeCapture() { }
    static Matrix4f pose(Matrix4f pose) { return com.kltyton.bedrockruntime.client.render.WorldRenderPose.cameraRelative(pose); }
    static void include(ModelPart.Cube cube, PartBounds.BoundsBuilder builder) {
        cube.compile(new com.mojang.blaze3d.vertex.PoseStack().last(),
                new com.kltyton.bedrockruntime.client.render.item.DiscardingVertexConsumer() {
                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) {
                        builder.include((float) x, (float) y, (float) z);
                        return this;
                    }
                }, 0, 0, 1, 1, 1, 1);
    }
}
