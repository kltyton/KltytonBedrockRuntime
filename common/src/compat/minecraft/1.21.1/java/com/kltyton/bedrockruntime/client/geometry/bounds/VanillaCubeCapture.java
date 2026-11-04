package com.kltyton.bedrockruntime.client.geometry.bounds;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

final class VanillaCubeCapture {
    private VanillaCubeCapture() { }
    static Matrix4f pose(Matrix4f pose) { return new Matrix4f(pose); }
    static void include(ModelPart.Cube cube, PartBounds.BoundsBuilder builder) {
        cube.compile(new com.mojang.blaze3d.vertex.PoseStack().last(),
                new com.kltyton.bedrockruntime.client.render.item.DiscardingVertexConsumer() {
                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
                        builder.include(x, y, z);
                        return this;
                    }
                }, 0, 0, -1);
    }
}
