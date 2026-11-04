package com.kltyton.bedrockruntime.client.particle.model.bake;

import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelUvSpace;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 1.21.1 bakery for ModelPart, GeckoLib 4 and existing poly_mesh geometry. */
public final class ModelParticleBakery {
    private ModelParticleBakery() {
    }

    public static ModelParticleModel hardcoded(ModelParticleModelType type, String id) {
        String raw = type.modelId();
        int separator = raw.indexOf('#');
        String layerName = separator >= 0 ? raw.substring(separator + 1) : "main";
        ResourceLocation location = ResourceLocation.tryParse(separator >= 0 ? raw.substring(0, separator) : raw);
        if (location == null || layerName.isBlank()) {
            throw new IllegalArgumentException(id + ": invalid hardcoded model id " + raw);
        }
        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(new ModelLayerLocation(location, layerName));
        return hardcodedRoot(id, root, type.texture(), type.material(), type.doubleSided());
    }

    public static ModelParticleModel hardcodedRoot(String id, ModelPart root, String texture,
                                                   ModelMaterial material, boolean doubleSided) {
        return NativeModelPartBakery.bake(id, root, texture, material, doubleSided);
    }

    public static ModelParticleModel gecko(ModelParticleModelType type, String id) {
        ResourceLocation location = ResourceLocation.tryParse(type.modelId());
        if (location == null) throw new IllegalArgumentException(id + ": invalid GeckoLib model id " + type.modelId());
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(location);
        if (model == null) throw new IllegalArgumentException(id + ": missing GeckoLib model " + type.modelId());
        EnhancedModelData data = MeshGeoBone.modelData(model);
        Map<String, TriangleMesh> meshes = data == null ? Map.of() : data.meshes();
        boolean meshDoubleSided = type.doubleSided() || (data != null && data.doubleSided());
        List<ModelParticlePart> parts = new ArrayList<>();
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        for (GeoBone bone : model.topLevelBones()) {
            collectBone(bone, new PoseStack(), builder, parts, meshes, type, meshDoubleSided);
        }
        TriangleMesh cubes = builder.build();
        if (cubes.triangleCount() > 0 || parts.isEmpty()) {
            parts.add(0, new ModelParticlePart(type.texture(), type.material(), ModelUvSpace.NORMALIZED,
                    cubes, new Matrix4f(), type.doubleSided()));
        }
        return new ModelParticleModel(id, type.texture(), parts, type.material(), type.doubleSided());
    }

    private static void collectBone(GeoBone bone, PoseStack poses, TriangleMeshBuilder builder,
                                    List<ModelParticlePart> parts, Map<String, TriangleMesh> meshes,
                                    ModelParticleModelType type, boolean meshDoubleSided) {
        poses.pushPose();
        var initial = bone.getInitialSnapshot();
        // The native animation processor saves the initial snapshot before the first controller update.
        // A still-unregistered resource bone can be copied without changing its shared initial state.
        if (initial == null) initial = bone.saveSnapshot();
        poses.translate(-initial.getOffsetX() / 16.0F, initial.getOffsetY() / 16.0F, initial.getOffsetZ() / 16.0F);
        RenderUtil.translateToPivotPoint(poses, bone);
        poses.mulPose(Axis.ZP.rotation(initial.getRotZ()));
        poses.mulPose(Axis.YP.rotation(initial.getRotY()));
        poses.mulPose(Axis.XP.rotation(initial.getRotX()));
        poses.scale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        RenderUtil.translateAwayFromPivotPoint(poses, bone);
        if (!Boolean.TRUE.equals(bone.shouldNeverRender())) {
            for (GeoCube cube : bone.getCubes()) {
                poses.pushPose();
                RenderUtil.translateToPivotPoint(poses, cube);
                RenderUtil.rotateMatrixAroundCube(poses, cube);
                RenderUtil.translateAwayFromPivotPoint(poses, cube);
                Matrix4f pose = poses.last().pose();
                boolean mirrored = pose.determinant3x3() < 0;
                for (GeoQuad quad : cube.quads()) {
                    if (quad == null) continue;
                    Vector3f normal = new Vector3f(quad.normal()).mul(poses.last().normal());
                    RenderUtil.fixInvertedFlatCube(cube, normal);
                    int normalIndex = builder.addNormal(normal.x, normal.y, normal.z);
                    int[] positions = new int[4];
                    int[] uvs = new int[4];
                    Vector3f position = new Vector3f();
                    for (int corner = 0; corner < 4; corner++) {
                        int source = mirrored && corner != 0 ? 4 - corner : corner;
                        GeoVertex vertex = quad.vertices()[source];
                        vertex.position().mulPosition(pose, position);
                        positions[corner] = builder.addPosition(position.x, position.y, position.z);
                        uvs[corner] = builder.addUv(vertex.texU(), vertex.texV());
                    }
                    builder.addQuad(positions[0], positions[1], positions[2], positions[3],
                            normalIndex, normalIndex, normalIndex, normalIndex, uvs[0], uvs[1], uvs[2], uvs[3]);
                }
                poses.popPose();
            }
        }
        if (!Boolean.TRUE.equals(bone.shouldNeverRender())) {
        TriangleMesh poly = meshes.get(bone.getName());
        if (poly != null) {
            parts.add(new ModelParticlePart(type.texture(), type.material(), ModelUvSpace.NORMALIZED,
                    poly, new Matrix4f(poses.last().pose()), meshDoubleSided));
        }
        }
        for (GeoBone child : bone.getChildBones()) {
            collectBone(child, poses, builder, parts, meshes, type, meshDoubleSided);
        }
        poses.popPose();
    }

    public static ModelParticleModel vanilla(String id, com.google.gson.JsonObject json,
                                             ModelParticleModelType type) {
        return NativeGeometryParticleBakery.bakeJson(id, json, type);
    }

    public static void registerProgrammatic(String id, ModelParticleModel model) {
        ModelParticleModels.register(id, model);
    }
}




