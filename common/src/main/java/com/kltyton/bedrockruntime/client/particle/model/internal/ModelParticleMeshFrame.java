package com.kltyton.bedrockruntime.client.particle.model.internal;

import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexSink;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Owned mesh pose and appearance captured before a material batch is drawn. */
public final class ModelParticleMeshFrame {
    private final TriangleMesh mesh;
    private final Matrix4f pose;
    private final Matrix3f normals;
    private final int color;
    private final int light;
    private final int overlay;
    private final boolean doubleSided;
    private final float u0;
    private final float v0;
    private final float du;
    private final float dv;

    public ModelParticleMeshFrame(TriangleMesh mesh, Matrix4f pose, Matrix3f normals,
                                  int color, int light, int overlay, boolean doubleSided,
                                  float u0, float v0, float du, float dv) {
        this.mesh = mesh;
        this.pose = new Matrix4f(pose);
        this.normals = new Matrix3f(normals);
        this.color = color;
        this.light = light;
        this.overlay = overlay;
        this.doubleSided = doubleSided;
        this.u0 = u0;
        this.v0 = v0;
        this.du = du;
        this.dv = dv;
    }

    public void emit(MeshVertexSink sink) {
        MeshVertexEmitter.emit(mesh, pose, normals, color, light, overlay, doubleSided,
                (x, y, z, c, u, v, o, l, nx, ny, nz) ->
                        sink.vertex(x, y, z, c, u0 + u * du, v0 + v * dv, o, l, nx, ny, nz));
    }
}

