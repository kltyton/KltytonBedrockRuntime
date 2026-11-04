package com.kltyton.bedrockruntime.client.particle.model.bake;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;

/** Collects complete native ModelPart vertices; modern setters finish before the next vertex begins. */
public final class ModelPartQuadCapture {
    private final TriangleMeshBuilder mesh = new TriangleMeshBuilder();
    private final float[][] positions = new float[4][3];
    private final float[][] normals = new float[4][3];
    private final float[][] uvs = new float[4][2];
    private final int[] colors = new int[4];
    private final int[] lights = new int[4];
    private int corner;
    private boolean active;

    public void vertex(float x, float y, float z, int defaultColor) {
        if (active) endVertex();
        positions[corner][0] = x;
        positions[corner][1] = y;
        positions[corner][2] = z;
        colors[corner] = defaultColor;
        lights[corner] = 0;
        uvs[corner][0] = uvs[corner][1] = 0.0F;
        normals[corner][0] = normals[corner][1] = normals[corner][2] = 0.0F;
        active = true;
    }

    public void color(int color) { colors[corner] = color; }
    public void uv(float u, float v) { uvs[corner][0] = u; uvs[corner][1] = v; }
    public void light(int u, int v) { lights[corner] = (u & 65535) | ((v & 65535) << 16); }
    public void normal(float x, float y, float z) {
        normals[corner][0] = x;
        normals[corner][1] = y;
        normals[corner][2] = z;
    }

    public void endVertex() {
        if (!active) throw new IllegalStateException("ModelPart ended a vertex before starting one");
        active = false;
        if (++corner == 4) {
            emitQuad();
            corner = 0;
        }
    }

    public TriangleMesh finish() {
        if (active) endVertex();
        if (corner != 0) throw new IllegalStateException("ModelPart emitted an incomplete quad");
        return mesh.build();
    }

    private void emitQuad() {
        int[] p = new int[4];
        int[] n = new int[4];
        int[] uv = new int[4];
        for (int index = 0; index < 4; index++) {
            p[index] = mesh.addPosition(positions[index][0], positions[index][1], positions[index][2],
                    colors[index], lights[index]);
            n[index] = mesh.addNormal(normals[index][0], normals[index][1], normals[index][2]);
            uv[index] = mesh.addUv(uvs[index][0], uvs[index][1]);
        }
        double ax = positions[1][0] - positions[0][0];
        double ay = positions[1][1] - positions[0][1];
        double az = positions[1][2] - positions[0][2];
        double bx = positions[2][0] - positions[0][0];
        double by = positions[2][1] - positions[0][1];
        double bz = positions[2][2] - positions[0][2];
        double nx = 0, ny = 0, nz = 0;
        for (float[] normal : normals) { nx += normal[0]; ny += normal[1]; nz += normal[2]; }
        // Native cube normals preserve the outward direction even after a mirrored part transform.
        boolean reverse = (ay * bz - az * by) * nx + (az * bx - ax * bz) * ny + (ax * by - ay * bx) * nz < 0;
        int b = reverse ? 3 : 1;
        int d = reverse ? 1 : 3;
        mesh.addQuad(p[0], p[b], p[2], p[d], n[0], n[b], n[2], n[d],
                uv[0], uv[b], uv[2], uv[d]);
    }
}

