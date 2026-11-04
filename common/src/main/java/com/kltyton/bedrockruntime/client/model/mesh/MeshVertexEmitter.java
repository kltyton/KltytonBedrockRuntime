package com.kltyton.bedrockruntime.client.model.mesh;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import org.joml.Matrix3fc;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Emits immutable triangle/quad meshes through the caller's quad vertex sink. */
public final class MeshVertexEmitter {
    private MeshVertexEmitter() { }

    public static void emit(TriangleMesh mesh, Matrix4fc pose, Matrix3fc normals, int color,
                            int light, int overlay, boolean doubleSided, MeshVertexSink sink) {
        boolean mirrored = pose.determinant3x3() < 0;
        Matrix3fc normalTransform = normals;
        if (!finite(normals)) {
            Matrix3f cofactor = pose.cofactor3x3(new Matrix3f());
            if (mirrored) { cofactor.scale(-1); }
            normalTransform = cofactor;
        }
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        int quad = 0;
        int triangle = 0;
        while (triangle < mesh.triangleCount()) {
            if (quad < mesh.quadCount() && mesh.quadCornerIndex(quad, 0) / 3 == triangle) {
                emitQuad(mesh, quad, pose, normalTransform, color, light, overlay, mirrored, false, sink, position, normal);
                if (doubleSided) {
                    emitQuad(mesh, quad, pose, normalTransform, color, light, overlay, !mirrored, true, sink, position, normal);
                }
                triangle += 2;
                quad++;
            }
            else {
                emitFace(mesh, triangle, pose, normalTransform, color, light, overlay, mirrored, false, sink, position, normal);
                if (doubleSided) {
                    emitFace(mesh, triangle, pose, normalTransform, color, light, overlay, !mirrored, true, sink, position, normal);
                }
                triangle++;
            }
        }
    }

    private static void emitQuad(TriangleMesh mesh, int quad, Matrix4fc pose, Matrix3fc normals,
                                 int color, int light, int overlay, boolean reverse, boolean back,
                                 MeshVertexSink sink, Vector3f position, Vector3f normal) {
        for (int vertex = 0; vertex < 4; vertex++) {
            int corner = reverse ? (vertex == 0 ? 0 : 4 - vertex) : vertex;
            int cornerIndex = mesh.quadCornerIndex(quad, corner);
            int triangle = cornerIndex / 3;
            int triangleCorner = cornerIndex % 3;
            pose.transformPosition(mesh.x(triangle, triangleCorner), mesh.y(triangle, triangleCorner),
                    mesh.z(triangle, triangleCorner), position);
            normals.transform(mesh.nx(triangle, triangleCorner), mesh.ny(triangle, triangleCorner),
                    mesh.nz(triangle, triangleCorner), normal);
            normalize(normal);
            if (back) {
                normal.negate();
            }
            sink.vertex(position.x, position.y, position.z, multiplyColor(color, mesh.color(triangle, triangleCorner)),
                    mesh.u(triangle, triangleCorner), mesh.v(triangle, triangleCorner), overlay,
                    maxLight(light, mesh.light(triangle, triangleCorner)), normal.x, normal.y, normal.z);
        }
    }

    private static void emitFace(TriangleMesh mesh, int triangle, Matrix4fc pose, Matrix3fc normals,
                                 int color, int light, int overlay, boolean reverse, boolean back,
                                 MeshVertexSink sink, Vector3f position, Vector3f normal) {
        for (int vertex = 0; vertex < 4; vertex++) {
            int corner = vertex == 3 ? 2 : vertex;
            if (reverse && corner != 0) {
                corner = 3 - corner;
            }
            pose.transformPosition(mesh.x(triangle, corner), mesh.y(triangle, corner), mesh.z(triangle, corner), position);
            normals.transform(mesh.nx(triangle, corner), mesh.ny(triangle, corner), mesh.nz(triangle, corner), normal);
            normalize(normal);
            if (back) {
                normal.negate();
            }
            sink.vertex(position.x, position.y, position.z, multiplyColor(color, mesh.color(triangle, corner)),
                    mesh.u(triangle, corner), mesh.v(triangle, corner), overlay,
                    maxLight(light, mesh.light(triangle, corner)), normal.x, normal.y, normal.z);
        }
    }

    private static int multiplyColor(int tint, int model) {
        if (model == 0xFFFFFFFF) return tint;
        if (tint == 0xFFFFFFFF) return model;
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int channel = (((tint >>> shift) & 255) * ((model >>> shift) & 255) + 127) / 255;
            result |= channel << shift;
        }
        return result;
    }

    private static int maxLight(int light, int baked) {
        if (baked == 0) return light;
        return Math.max(light & 65535, baked & 65535)
                | (Math.max((light >>> 16) & 65535, (baked >>> 16) & 65535) << 16);
    }


    private static boolean finite(Matrix3fc matrix) {
        return Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m01()) && Float.isFinite(matrix.m02())
                && Float.isFinite(matrix.m10()) && Float.isFinite(matrix.m11()) && Float.isFinite(matrix.m12())
                && Float.isFinite(matrix.m20()) && Float.isFinite(matrix.m21()) && Float.isFinite(matrix.m22());
    }

    private static void normalize(Vector3f normal) {
        if (!Float.isFinite(normal.x) || !Float.isFinite(normal.y) || !Float.isFinite(normal.z)) {
            throw new IllegalArgumentException("Mesh normal transform cannot be represented by finite coordinates");
        }
        double squared = (double) normal.x * normal.x + (double) normal.y * normal.y + (double) normal.z * normal.z;
        if (squared > 0) {
            double inverse = 1.0 / Math.sqrt(squared);
            normal.set((float) (normal.x * inverse), (float) (normal.y * inverse), (float) (normal.z * inverse));
        }
    }
}
