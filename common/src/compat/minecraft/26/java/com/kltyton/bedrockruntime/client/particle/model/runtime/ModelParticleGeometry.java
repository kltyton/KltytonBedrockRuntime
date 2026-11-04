package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Captured mesh, pose, material and UV mapping; delayed drawing never reads a live particle. */
public final class ModelParticleGeometry implements SubmitNodeCollector.CustomGeometryRenderer {
    private final TriangleMesh mesh;
    private final Matrix4f pose;
    private final Matrix3f normals;
    private final RenderType renderType;
    private final int color;
    private final int light;
    private final boolean doubleSided;
    private final float u0;
    private final float v0;
    private final float du;
    private final float dv;

    public ModelParticleGeometry(TriangleMesh mesh, Matrix4f pose, Matrix3f normals,
                                 RenderType renderType, int color, int light, boolean doubleSided,
                                 float u0, float v0, float du, float dv) {
        this.mesh = mesh;
        this.pose = new Matrix4f(pose);
        this.normals = new Matrix3f(normals);
        this.renderType = renderType;
        this.color = color;
        this.light = light;
        this.doubleSided = doubleSided;
        this.u0 = u0;
        this.v0 = v0;
        this.du = du;
        this.dv = dv;
    }

    public void submit(SubmitNodeCollector collector, PoseStack identity) {
        collector.submitCustomGeometry(identity, renderType, this);
    }

    @Override
    public void render(PoseStack.Pose ignored, VertexConsumer buffer) {
        MeshVertexEmitter.emit(mesh, pose, normals, color, light, OverlayTexture.NO_OVERLAY,
                doubleSided, (x, y, z, c, u, v, o, l, nx, ny, nz) ->
                        buffer.addVertex(x, y, z, c, u0 + u * du, v0 + v * dv, o, l, nx, ny, nz));
    }
}

