package com.kltyton.bedrockruntime.client.compat.geckolib.model;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Map;

public final class EnhancedBoneMeshes {
    private static final Cache<GeoBone, Attachment> ATTACHMENTS = CacheBuilder.newBuilder().weakKeys().build();

    private EnhancedBoneMeshes() {
    }

    public static void attach(BakedGeoModel model, EnhancedModelData data) {
        if (data == null || data.meshes().isEmpty()) {
            return;
        }
        boolean doubleSided = data.doubleSided();
        for (Map.Entry<String, TriangleMesh> entry : data.meshes().entrySet()) {
            GeoBone bone = model.getBone(entry.getKey()).orElseThrow(
                    () -> new IllegalArgumentException(data.resource() + ": missing mesh bone '" + entry.getKey() + "'"));
            ATTACHMENTS.put(bone, new Attachment(entry.getValue(), data, doubleSided,
                    data.captureData().get(entry.getKey())));
        }
    }

    public static void render(GeoBone bone, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay, int color) {
        Attachment attachment = bone == null ? null : ATTACHMENTS.getIfPresent(bone);
        if (attachment == null || (bone.frameSnapshot != null && bone.frameSnapshot.isHidden())) {
            return;
        }
        MeshVertexEmitter.emit(attachment.mesh(), poseStack.last().pose(), poseStack.last().normal(), color,
                light, overlay, attachment.doubleSided(), consumer::addVertex);
    }

    public static TriangleMesh mesh(GeoBone bone) {
        Attachment attachment = bone == null ? null : ATTACHMENTS.getIfPresent(bone);
        return attachment == null ? null : attachment.mesh();
    }

    public static EnhancedModelData modelData(GeoBone bone) {
        Attachment attachment = bone == null ? null : ATTACHMENTS.getIfPresent(bone);
        return attachment == null ? null : attachment.data();
    }

    public static MeshCaptureData captureData(GeoBone bone) {
        Attachment attachment = bone == null ? null : ATTACHMENTS.getIfPresent(bone);
        return attachment == null ? null : attachment.captureData();
    }

    public static boolean doubleSided(GeoBone bone) {
        Attachment attachment = bone == null ? null : ATTACHMENTS.getIfPresent(bone);
        return attachment != null && attachment.doubleSided();
    }

    private record Attachment(TriangleMesh mesh, EnhancedModelData data, boolean doubleSided,
                              MeshCaptureData captureData) {
    }
}
