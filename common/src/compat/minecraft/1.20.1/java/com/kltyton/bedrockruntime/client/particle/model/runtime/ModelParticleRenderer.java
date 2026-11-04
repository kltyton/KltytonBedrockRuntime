package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleMeshFrame;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelUvSpace;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Captures model geometry independently of the native billboard vertex format. */
public final class ModelParticleRenderer {
    private ModelParticleRenderer() {
    }

    public static void render(ModelMolangParticle particle, VertexConsumer buffer, Camera camera, float partialTick) {
        var state = particle.kltyton_bedrock_runtime$modelParticleState();
        ModelParticleModel model = state.model();
        if (model == null) {
            return;
        }
        TextureAtlasSprite sprite = ModelParticleTextures.sprite(model.texture());

        float width = size(particle, 0, partialTick);
        float height = size(particle, 1, partialTick);
        float depth = size(particle, 2, partialTick);
        if (width == 0.0F && height == 0.0F && depth == 0.0F) {
            return;
        }

        Vector3f position = particle.getRenderWorldPosition(new Vector3f(), partialTick);
        Vec3 cameraPosition = camera.getPosition();
        position.sub((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);

        Matrix4f pose = new Matrix4f().translation(position);
        Matrix3f basis = new Matrix3f();
        particle.getFacingCameraMode().setRenderBasis(particle, basis, new Vector3f(), camera, partialTick);
        Float roll = particle.getRotation();
        if (roll != null && Math.abs(roll) > 1.0E-6F) {
            basis.rotateZ(roll);
        }
        pose.m00(basis.m00()).m01(basis.m01()).m02(basis.m02())
                .m10(basis.m10()).m11(basis.m11()).m12(basis.m12())
                .m20(basis.m20()).m21(basis.m21()).m22(basis.m22());
        pose.translate(width * -0.5F, height * -0.5F, depth * -0.5F);
        pose.scale(width, height, depth);

        int color = color(particle);
        int light = particle.kltyton_bedrock_runtime$modelParticleLight(partialTick);
        int overlay = OverlayTexture.NO_OVERLAY;
        for (ModelParticlePart part : model.parts()) {
            if (part.mesh().triangleCount() == 0) continue;
            Matrix4f partPose = part.appendTransform(new Matrix4f(pose));
            Matrix3f normals = partPose.normal(new Matrix3f());
            boolean doubleSided = part.doubleSided() || model.doubleSided();
            TextureAtlasSprite partSprite = part.texture().isEmpty() ? sprite
                    : ModelParticleTextures.sprite(part.texture());
            boolean normalized = part.uvSpace() == ModelUvSpace.NORMALIZED;
            if (normalized && partSprite == null) {
                continue;
            }
            var atlas = partSprite == null ? TextureAtlas.LOCATION_BLOCKS : partSprite.atlasLocation();
            float u0 = normalized ? partSprite.getU0() : 0.0F;
            float v0 = normalized ? partSprite.getV0() : 0.0F;
            float du = normalized ? partSprite.getU1() - u0 : 1.0F;
            float dv = normalized ? partSprite.getV1() - v0 : 1.0F;
            ModelParticleBatch.capture(ModelParticleRenderTypes.forMaterial(part.material(), atlas),
                    part.material().translucent(), new ModelParticleMeshFrame(part.mesh(), partPose,
                            normals, color, light, overlay, doubleSided, u0, v0, du, dv));
        }
    }

    private static float size(ModelMolangParticle particle, int axis, float partialTick) {
        Float value = particle.getBillboardSize(axis);
        if (value == null) {
            float fallback = axis < 2 ? particle.getQuadSize(partialTick)
                    : Math.max(particle.getQuadSize(partialTick), 1.0E-4F);
            return Float.isFinite(fallback) ? fallback : 0.0F;
        }
        return Float.isFinite(value) ? value : 0.0F;
    }

    private static int color(ModelMolangParticle particle) {
        int r = channel(particle.getColorComponent(0));
        int g = channel(particle.getColorComponent(1));
        int b = channel(particle.getColorComponent(2));
        int a = channel(particle.getColorComponent(3));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(Float value) {
        if (value == null || !Float.isFinite(value)) {
            return 255;
        }
        return Math.max(0, Math.min(255, (int) (value * 255.0F)));
    }
}




