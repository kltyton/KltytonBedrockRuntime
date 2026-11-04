package com.kltyton.bedrockruntime.client.particle.model.bake;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.util.RenderUtil;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.EnhancedBakedGeoModel;
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
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 26.2 bakery for ModelPart, GeckoLib 5 and existing poly_mesh geometry. */
public final class ModelParticleBakery {
    private ModelParticleBakery() {
    }

    public static ModelParticleModel hardcoded(ModelParticleModelType type, String id) {
        String raw = type.modelId();
        int separator = raw.indexOf('#');
        String layerName = separator >= 0 ? raw.substring(separator + 1) : "main";
        Identifier location = Identifier.tryParse(separator >= 0 ? raw.substring(0, separator) : raw);
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
        Identifier location = Identifier.tryParse(type.modelId());
        if (location == null) throw new IllegalArgumentException(id + ": invalid GeckoLib model id " + type.modelId());
        BakedGeoModel model = GeckoLibResources.getBakedModels()
                .getModel(GeckoLibResources.stripPrefixAndSuffix(location));
        if (model == null) throw new IllegalArgumentException(id + ": missing GeckoLib model " + type.modelId());
        var data = model instanceof EnhancedBakedGeoModel enhanced ? enhanced.extension() : null;
        Map<String, TriangleMesh> meshes = data == null ? Map.of() : data.meshes();
        boolean meshDoubleSided = type.doubleSided() || (data != null && data.doubleSided());
        List<ModelParticlePart> parts = new ArrayList<>();
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        for (GeoBone bone : model.topLevelBones()) {
            collectBone(bone, new Matrix4f(), builder, parts, meshes, type, meshDoubleSided);
        }
        TriangleMesh cubes = builder.build();
        if (cubes.triangleCount() > 0 || parts.isEmpty()) {
            parts.add(0, new ModelParticlePart(type.texture(), type.material(), ModelUvSpace.NORMALIZED,
                    cubes, new Matrix4f(), type.doubleSided()));
        }
        return new ModelParticleModel(id, type.texture(), parts, type.material(), type.doubleSided());
    }

    private static void collectBone(GeoBone bone, Matrix4f parent, TriangleMeshBuilder builder,
                                    List<ModelParticlePart> parts, Map<String, TriangleMesh> meshes,
                                    ModelParticleModelType type, boolean meshDoubleSided) {
        Matrix4f boneMatrix = new Matrix4f(parent).mul(bindLocal(bone));
        PoseStack poses = new PoseStack();
        poses.mulPose(boneMatrix);
        poses.last().normal().set(boneMatrix.normal(new Matrix3f()));
        if (bone instanceof CuboidGeoBone cuboid) {
            for (GeoCube cube : cuboid.cubes) {
                poses.pushPose();
                cube.translateToPivotPoint(poses);
                cube.rotate(poses);
                cube.translateAwayFromPivotPoint(poses);
                Matrix4f pose = poses.last().pose();
                boolean mirrored = pose.determinant3x3() < 0;
                for (GeoQuad quad : cube.quads()) {
                    if (quad == null) continue;
                    Vector3f normal = new Vector3f(quad.normalVec()).mul(poses.last().normal());
                    RenderUtil.fixInvertedFlatCube(cube, normal);
                    int normalIndex = builder.addNormal(normal.x, normal.y, normal.z);
                    int[] positions = new int[4];
                    int[] uvs = new int[4];
                    Vector3f position = new Vector3f();
                    for (int corner = 0; corner < 4; corner++) {
                        int source = mirrored && corner != 0 ? 4 - corner : corner;
                        GeoVertex vertex = quad.vertices()[source];
                        pose.transformPosition(vertex.posX(), vertex.posY(), vertex.posZ(), position);
                        positions[corner] = builder.addPosition(position.x, position.y, position.z);
                        uvs[corner] = builder.addUv(vertex.texU(), vertex.texV());
                    }
                    builder.addQuad(positions[0], positions[1], positions[2], positions[3],
                            normalIndex, normalIndex, normalIndex, normalIndex, uvs[0], uvs[1], uvs[2], uvs[3]);
                }
                poses.popPose();
            }
        }
        
        TriangleMesh poly = meshes.get(bone.name());
        if (poly != null) {
            parts.add(new ModelParticlePart(type.texture(), type.material(), ModelUvSpace.NORMALIZED,
                    poly, new Matrix4f(boneMatrix), meshDoubleSided));
        }
        
        for (GeoBone child : bone.children()) {
            collectBone(child, boneMatrix, builder, parts, meshes, type, meshDoubleSided);
        }

    }

    private static Matrix4f bindLocal(GeoBone bone) {
        PoseStack poses = new PoseStack();
        poses.translate(bone.pivotX() / 16.0F, bone.pivotY() / 16.0F, bone.pivotZ() / 16.0F);
        poses.mulPose(Axis.ZP.rotation(bone.baseRotZ()));
        poses.mulPose(Axis.YP.rotation(bone.baseRotY()));
        poses.mulPose(Axis.XP.rotation(bone.baseRotX()));
        poses.translate(-bone.pivotX() / 16.0F, -bone.pivotY() / 16.0F, -bone.pivotZ() / 16.0F);
        return new Matrix4f(poses.last().pose());
    }

    public static ModelParticleModel vanilla(String id, com.google.gson.JsonObject json,
                                             ModelParticleModelType type) {
        return NativeGeometryParticleBakery.bakeJson(id, json, type);
    }

    public static void registerProgrammatic(String id, ModelParticleModel model) {
        ModelParticleModels.register(id, model);
    }
}
