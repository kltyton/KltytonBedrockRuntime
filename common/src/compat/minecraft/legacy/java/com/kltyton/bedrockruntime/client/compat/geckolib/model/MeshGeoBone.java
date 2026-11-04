package com.kltyton.bedrockruntime.client.compat.geckolib.model;

import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Bone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public final class MeshGeoBone {
    private static final Cache<GeoBone, Attachment> BONES = CacheBuilder.newBuilder().weakKeys().build();
    private static final Cache<BakedGeoModel, ModelAttachment> MODELS = CacheBuilder.newBuilder().weakKeys().build();

    private MeshGeoBone() {
    }

    public static void attach(BakedGeoModel model, EnhancedModelData data, String resource,
                              boolean doubleSided, Map<String, Bone> rawBones) {
        Map<String, Bone> raw = rawBones == null ? Map.of() : Map.copyOf(rawBones);
        boolean sided = doubleSided || data.doubleSided();
        MODELS.put(model, new ModelAttachment(data, resource, raw));
        for (GeoBone root : model.topLevelBones()) {
            attachBone(root, data, resource, sided, raw);
        }
    }

    public static TriangleMesh mesh(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment == null ? null : attachment.mesh;
    }

    public static String resource(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment == null ? null : attachment.resource;
    }

    public static long generation(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment == null ? 0L : attachment.generation;
    }

    public static boolean doubleSided(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment != null && attachment.doubleSided;
    }

    public static MeshCaptureData captureData(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment == null ? null : attachment.captureData;
    }

    public static Bone rawBone(GeoBone bone) {
        Attachment attachment = attachment(bone);
        return attachment == null ? null : attachment.rawBone;
    }

    public static Map<String, LocatorValue> locators(GeoBone bone) {
        Bone raw = rawBone(bone);
        return raw == null || raw.locators() == null ? Map.of() : Map.copyOf(raw.locators());
    }

    public static EnhancedModelData modelData(BakedGeoModel model) {
        ModelAttachment attachment = model == null ? null : MODELS.getIfPresent(model);
        return attachment == null ? null : attachment.data;
    }

    public static boolean isMeshBone(GeoBone bone) {
        return attachment(bone) != null;
    }

    private static void attachBone(GeoBone bone, EnhancedModelData data, String resource,
                                   boolean doubleSided, Map<String, Bone> rawBones) {
        String name = bone.getName();
        TriangleMesh mesh = data.meshes().get(name);
        MeshCaptureData capture = mesh == null ? null : data.captureData().get(name);
        BONES.put(bone, new Attachment(resource, data.generation(), mesh, doubleSided, capture, rawBones.get(name)));
        for (GeoBone child : bone.getChildBones()) {
            attachBone(child, data, resource, doubleSided, rawBones);
        }
    }

    private static Attachment attachment(GeoBone bone) {
        return bone == null ? null : BONES.getIfPresent(bone);
    }

    private record Attachment(String resource, long generation, TriangleMesh mesh, boolean doubleSided,
                              MeshCaptureData captureData, Bone rawBone) {
    }

    private record ModelAttachment(EnhancedModelData data, String resource, Map<String, Bone> rawBones) {
    }
}
