package com.kltyton.bedrockruntime.client.compat.geckolib.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import com.kltyton.bedrockruntime.model.binding.ModelConsumers;
import com.kltyton.bedrockruntime.model.definition.ModelGeometry;
import com.kltyton.bedrockruntime.model.definition.ModelGeometryBuilder;
import com.kltyton.bedrockruntime.model.optimization.CompiledGroup;
import com.kltyton.bedrockruntime.model.optimization.CompiledModelPlan;
import com.kltyton.bedrockruntime.model.optimization.CompiledModelPlanner;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GL5CompiledGeometry {
    private static final String MATERIAL = "geckolib:default";
    private static final Map<BakedGeoModel, CompiledModelPlan> PLANS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private GL5CompiledGeometry() {
    }

    public static CompiledModelPlan compile(EnhancedModelData data, BakedGeoModel model, ModelConsumers consumers) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(model, "model");
        ModelGeometry logical = logicalGeometry(data, model);
        EnhancedModelData.Mode mode = nativeTraversal(model.topLevelBones()) ? data.mode() : EnhancedModelData.Mode.TREE;
        CompiledModelPlan plan = CompiledModelPlanner.plan(data.resource(), data.generation(), mode,
                logical, data, consumers);
        PLANS.put(model, plan);
        return plan;
    }

    private static boolean nativeTraversal(GeoBone[] bones) {
        for (GeoBone bone : bones) {
            if (!(bone instanceof CuboidGeoBone) || !nativeTraversal(bone.children())) {
                return false;
            }
        }
        return true;
    }

    public static CompiledModelPlan planFor(BakedGeoModel model) {
        return model == null ? null : PLANS.get(model);
    }

    public static void forget(BakedGeoModel model) {
        if (model != null) {
            PLANS.remove(model);
        }
    }

    public static <R extends GeoRenderState> void render(CompiledModelPlan plan, RenderPassInfo<R> pass,
                                                         VertexConsumer consumer, int light, int overlay, int color) {
        BakedGeoModel model = pass.model();
        Map<String, List<CompiledGroup>> renderAt = new LinkedHashMap<>();
        Set<String> suppressed = new HashSet<>();
        for (CompiledGroup group : plan.groups()) {
            GroupState state = evaluate(model, group);
            if (state != GroupState.FALLBACK) {
                suppressed.addAll(group.sourceBones());
            }
            if (state == GroupState.USE) {
                renderAt.computeIfAbsent(group.anchorBone(), ignored -> new ArrayList<>()).add(group);
            }
        }
        PoseStack poses = pass.poseStack();
        for (GeoBone root : model.topLevelBones()) {
            renderBone(plan, root, pass, poses, consumer, light, overlay, color, suppressed, renderAt);
        }
    }

    private static <R extends GeoRenderState> void renderBone(CompiledModelPlan plan, GeoBone bone, RenderPassInfo<R> pass, PoseStack poses,
                                   VertexConsumer consumer, int light, int overlay, int color,
                                   Set<String> suppressed, Map<String, List<CompiledGroup>> renderAt) {
        poses.pushPose();
        try {
            RenderUtil.prepMatrixForBoneAndUpdateListeners(poses, bone, pass);
            List<CompiledGroup> groups = renderAt.get(bone.name());
            if (groups != null) {
                for (CompiledGroup group : groups) {
                    MeshVertexEmitter.emit(group.mesh(), poses.last().pose(), poses.last().normal(), color,
                            light, overlay, plan.doubleSided(), consumer::addVertex);
                }
            }
            if (!suppressed.contains(bone.name())) {
                bone.render(pass, poses, consumer, light, overlay, color);
            }
            if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
                for (GeoBone child : bone.children()) {
                    renderBone(plan, child, pass, poses, consumer, light, overlay, color, suppressed, renderAt);
                }
            }
        } finally {
            poses.popPose();
        }
    }

    private static GroupState evaluate(BakedGeoModel model, CompiledGroup group) {
        boolean anyVisible = false;
        boolean anyHidden = false;
        for (String source : group.sourceBones()) {
            GeoBone bone = model.getBone(source).orElse(null);
            if (bone == null) {
                return GroupState.FALLBACK;
            }
            if (visible(bone)) {
                anyVisible = true;
            } else {
                anyHidden = true;
            }
        }
        if (!anyVisible) {
            return GroupState.HIDDEN;
        }
        if (anyHidden) {
            return GroupState.FALLBACK;
        }
        for (String dependency : group.dependencyBones()) {
            GeoBone bone = model.getBone(dependency).orElse(null);
            if (bone == null) {
                return GroupState.FALLBACK;
            }
            if (bone.frameSnapshot != null
                    && (bone.frameSnapshot.hasTranslation() || bone.frameSnapshot.hasRotation()
                    || bone.frameSnapshot.hasScale())) {
                return GroupState.FALLBACK;
            }
        }
        return GroupState.USE;
    }

    private static boolean visible(GeoBone bone) {
        if (bone.frameSnapshot != null && bone.frameSnapshot.isHidden()) {
            return false;
        }
        for (GeoBone parent = bone.parent(); parent != null; parent = parent.parent()) {
            if (parent.frameSnapshot != null && parent.frameSnapshot.areChildrenHidden()) {
                return false;
            }
        }
        return true;
    }

    private static ModelGeometry logicalGeometry(EnhancedModelData data, BakedGeoModel model) {
        boolean immutable = data.mode() == EnhancedModelData.Mode.BAKED && !data.staticBones().isEmpty();
        ModelGeometryBuilder builder = new ModelGeometryBuilder(data.resource(), immutable);
        boolean includeParts = immutable;
        for (GeoBone root : model.topLevelBones()) {
            appendBone(builder, data, root, includeParts);
        }
        return builder.build();
    }

    private static void appendBone(ModelGeometryBuilder builder, EnhancedModelData data, GeoBone bone,
                                   boolean includeParts) {
        String parent = bone.parent() == null ? null : bone.parent().name();
        builder.addBone(bone.name(), parent, bindLocal(bone),
                new Vector3f(bone.pivotX(), bone.pivotY(), bone.pivotZ()));
        if (includeParts && bone instanceof CuboidGeoBone cuboid) {
            for (GeoCube cube : cuboid.cubes) {
                TriangleMesh mesh = cubeMesh(cube);
                if (mesh.triangleCount() > 0) {
                    builder.addPart(bone.name(), MATERIAL, mesh, new Matrix4f());
                }
            }
            TriangleMesh polyMesh = data.meshes().get(bone.name());
            if (polyMesh != null) {
                builder.addPart(bone.name(), MATERIAL, polyMesh, new Matrix4f());
            }
        }
        for (GeoBone child : bone.children()) {
            appendBone(builder, data, child, includeParts);
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

    private static TriangleMesh cubeMesh(GeoCube cube) {
        Matrix4f transform = cubeTransform(cube);
        Matrix3f normalMatrix = new Matrix3f(transform);
        if (normalMatrix.determinant() != 0.0F) {
            normalMatrix.invert().transpose();
        }
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        if (cube.quads() == null) {
            return builder.build();
        }
        for (GeoQuad quad : cube.quads()) {
            if (quad == null || quad.vertices() == null || quad.vertices().length < 4) {
                continue;
            }
            Vector3f normal = new Vector3f(quad.normalX(), quad.normalY(), quad.normalZ());
            normalMatrix.transform(normal);
            if (normal.lengthSquared() > 1.0E-20F) {
                normal.normalize();
            } else {
                normal.set(0.0F, 1.0F, 0.0F);
            }
            int[] positions = new int[4];
            int[] normals = new int[4];
            int[] uvs = new int[4];
            for (int i = 0; i < 4; i++) {
                GeoVertex vertex = quad.vertices()[i];
                Vector3f position = transform.transformPosition(vertex.posX(), vertex.posY(), vertex.posZ(), new Vector3f());
                positions[i] = builder.addPosition(position.x(), position.y(), position.z());
                normals[i] = builder.addNormal(normal.x(), normal.y(), normal.z());
                uvs[i] = builder.addUv(vertex.texU(), vertex.texV());
            }
            builder.addQuad(positions[0], positions[1], positions[2], positions[3],
                    normals[0], normals[1], normals[2], normals[3],
                    uvs[0], uvs[1], uvs[2], uvs[3]);
        }
        return builder.build();
    }

    private static Matrix4f cubeTransform(GeoCube cube) {
        PoseStack poses = new PoseStack();
        float pivotX = (float) cube.pivot().x() / 16.0F;
        float pivotY = (float) cube.pivot().y() / 16.0F;
        float pivotZ = (float) cube.pivot().z() / 16.0F;
        poses.translate(pivotX, pivotY, pivotZ);
        poses.mulPose(Axis.ZP.rotation((float) cube.rotation().z()));
        poses.mulPose(Axis.YP.rotation((float) cube.rotation().y()));
        poses.mulPose(Axis.XP.rotation((float) cube.rotation().x()));
        poses.translate(-pivotX, -pivotY, -pivotZ);
        return new Matrix4f(poses.last().pose());
    }

    private enum GroupState {
        USE,
        HIDDEN,
        FALLBACK
    }
}




