package com.kltyton.bedrockruntime.model.definition;

import java.util.Objects;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;

import org.joml.Matrix4f;

public final class ModelGeometryPart {
    private final String material;
    private final TriangleMesh mesh;
    private final Matrix4f localTransform;

    public ModelGeometryPart(String material, TriangleMesh mesh, Matrix4f localTransform) {
        this.material = material == null ? "" : material;
        this.mesh = Objects.requireNonNull(mesh, "mesh");
        this.localTransform = new Matrix4f(Objects.requireNonNull(localTransform, "localTransform"));
        if (!finite(this.localTransform)) {
            throw new IllegalArgumentException("part transform must be finite");
        }
    }

    public String material() {
        return material;
    }

    public TriangleMesh mesh() {
        return mesh;
    }

    public Matrix4f localTransform() {
        return new Matrix4f(localTransform);
    }

    public int triangleCount() {
        return mesh.triangleCount();
    }

    private static boolean finite(Matrix4f matrix) {
        return Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m01()) && Float.isFinite(matrix.m02()) && Float.isFinite(matrix.m03())
                && Float.isFinite(matrix.m10()) && Float.isFinite(matrix.m11()) && Float.isFinite(matrix.m12()) && Float.isFinite(matrix.m13())
                && Float.isFinite(matrix.m20()) && Float.isFinite(matrix.m21()) && Float.isFinite(matrix.m22()) && Float.isFinite(matrix.m23())
                && Float.isFinite(matrix.m30()) && Float.isFinite(matrix.m31()) && Float.isFinite(matrix.m32()) && Float.isFinite(matrix.m33());
    }
}
