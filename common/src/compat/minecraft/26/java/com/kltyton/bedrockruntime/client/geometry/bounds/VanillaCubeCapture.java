package com.kltyton.bedrockruntime.client.geometry.bounds;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

final class VanillaCubeCapture {
    private VanillaCubeCapture() { }
    static Matrix4f pose(Matrix4f pose) { return new Matrix4f(pose); }
    static void include(ModelPart.Cube cube, PartBounds.BoundsBuilder builder) {
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                builder.include(vertex.worldX(), vertex.worldY(), vertex.worldZ());
            }
        }
    }
}
