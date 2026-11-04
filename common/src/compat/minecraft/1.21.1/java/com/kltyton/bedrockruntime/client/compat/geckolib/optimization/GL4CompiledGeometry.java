package com.kltyton.bedrockruntime.client.compat.geckolib.optimization;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

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

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.animation.state.BoneSnapshot;

public final class GL4CompiledGeometry {
    private static final String MATERIAL = "geckolib:default";
    private static final Map<GeoBone, CompiledModel> MODELS = Collections.synchronizedMap(new WeakHashMap<>());

    private GL4CompiledGeometry() {
    }

    public static CompiledModel compile(EnhancedModelData data, BakedGeoModel model, ModelConsumers consumers) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(model, "model");
        ModelGeometry logical = logicalGeometry(data, model);
        CompiledModelPlan plan = CompiledModelPlanner.plan(data.resource(), data.generation(), data.mode(),
                logical, data, consumers);
        Map<String, GeoBone> bones = new LinkedHashMap<>();
        for (GeoBone root : model.topLevelBones()) {
            collectBones(root, bones);
        }
        CompiledModel compiled = new CompiledModel(plan, model);
        if (plan.compiled()) {
            for (GeoBone bone : bones.values()) {
                MODELS.put(bone, compiled);
            }
        }
        return compiled;
    }

    public static CompiledModel modelFor(GeoBone bone) {
        return bone == null ? null : MODELS.get(bone);
    }

    public static CompiledModelPlan planFor(GeoBone bone) {
        CompiledModel model = modelFor(bone);
        return model == null ? null : model.plan();
    }

    public static boolean shouldSuppress(GeoBone bone) {
        CompiledModel model = modelFor(bone);
        return model != null && model.shouldSuppress(bone);
    }

    public static boolean intercept(GeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                   int color, int light, int overlay) {
        boolean suppress = shouldSuppress(bone);
        renderAtAnchor(bone, poseStack, buffer, color, light, overlay);
        return suppress;
    }

    public static boolean renderAtAnchor(GeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                         int color, int light, int overlay) {
        CompiledModel model = modelFor(bone);
        return model != null && model.renderAtAnchor(bone, poseStack, buffer, color, light, overlay);
    }

    public static void forget(GeoBone bone) {
        if (bone != null) {
            MODELS.remove(bone);
        }
    }

    public static final class CompiledModel {
        private final CompiledModelPlan plan;
        private final WeakReference<BakedGeoModel> model;

        private CompiledModel(CompiledModelPlan plan, BakedGeoModel model) {
            this.plan = plan;
            this.model = new WeakReference<>(model);
        }

        public CompiledModelPlan plan() {
            return plan;
        }

        public Map<String, GeoBone> bones() {
            BakedGeoModel current = model.get();
            if (current == null) {
                return Map.of();
            }
            Map<String, GeoBone> bones = new LinkedHashMap<>();
            for (GeoBone root : current.topLevelBones()) {
                collectBones(root, bones);
            }
            return Map.copyOf(bones);
        }

        public GeoBone bone(String name) {
            BakedGeoModel current = model.get();
            return current == null ? null : current.getBone(name).orElse(null);
        }

        public boolean shouldSuppress(GeoBone bone) {
            if (bone == null) {
                return false;
            }
            List<CompiledGroup> groups = plan.groupsForSource(bone.getName());
            for (CompiledGroup group : groups) {
                if (evaluate(group) != GroupState.FALLBACK) {
                    return true;
                }
            }
            return false;
        }

        public boolean renderAtAnchor(GeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                      int color, int light, int overlay) {
            if (bone == null) {
                return false;
            }
            boolean rendered = false;
            for (CompiledGroup group : plan.groupsAtAnchor(bone.getName())) {
                if (evaluate(group) != GroupState.USE) {
                    continue;
                }
                MeshVertexEmitter.emit(group.mesh(), poseStack.last().pose(), poseStack.last().normal(), color,
                        light, overlay, plan.doubleSided(), buffer::addVertex);
                rendered = true;
            }
            return rendered;
        }

        private GroupState evaluate(CompiledGroup group) {
            BakedGeoModel current = model.get();
            if (current == null) {
                return GroupState.FALLBACK;
            }
            boolean anyVisible = false;
            boolean anyHidden = false;
            for (String source : group.sourceBones()) {
                GeoBone bone = current.getBone(source).orElse(null);
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
                GeoBone bone = current.getBone(dependency).orElse(null);
                if (bone == null || !atBindPose(bone)) {
                    return GroupState.FALLBACK;
                }
            }
            return GroupState.USE;
        }
    }

    private static void collectBones(GeoBone bone, Map<String, GeoBone> bones) {
        bones.put(bone.getName(), bone);
        for (GeoBone child : bone.getChildBones()) {
            collectBones(child, bones);
        }
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
        String parent = bone.getParent() == null ? null : bone.getParent().getName();
        builder.addBone(bone.getName(), parent, bindLocal(bone),
                new Vector3f(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ()));
        if (includeParts) {
            for (GeoCube cube : bone.getCubes()) {
                TriangleMesh mesh = cubeMesh(cube);
                if (mesh.triangleCount() > 0) {
                    builder.addPart(bone.getName(), MATERIAL, mesh, new Matrix4f());
                }
            }
            TriangleMesh polyMesh = data.meshes().get(bone.getName());
            if (polyMesh != null) {
                builder.addPart(bone.getName(), MATERIAL, polyMesh, new Matrix4f());
            }
        }
        for (GeoBone child : bone.getChildBones()) {
            appendBone(builder, data, child, includeParts);
        }
    }

    private static Matrix4f bindLocal(GeoBone bone) {
        PoseStack poses = new PoseStack();
        poses.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
        poses.mulPose(Axis.ZP.rotation(bone.getRotZ()));
        poses.mulPose(Axis.YP.rotation(bone.getRotY()));
        poses.mulPose(Axis.XP.rotation(bone.getRotX()));
        poses.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
        return new Matrix4f(poses.last().pose());
    }

    private static TriangleMesh cubeMesh(GeoCube cube) {
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        if (cube.quads() == null) {
            return builder.build();
        }
        Matrix4f transform = cubeTransform(cube);
        Matrix3f normalMatrix = new Matrix3f(transform);
        if (normalMatrix.determinant() != 0.0F) {
            normalMatrix.invert().transpose();
        }
        for (GeoQuad quad : cube.quads()) {
            if (quad == null || quad.vertices() == null || quad.vertices().length < 4) {
                continue;
            }
            Vector3f normal = new Vector3f(quad.normal());
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
                Vector3f source = vertex.position();
                Vector3f position = transform.transformPosition(source.x(), source.y(), source.z(), new Vector3f());
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

    private static boolean visible(GeoBone bone) {
        if (bone.isHidden()) {
            return false;
        }
        for (GeoBone parent = bone.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.isHidingChildren()) {
                return false;
            }
        }
        return true;
    }

    private static boolean atBindPose(GeoBone bone) {
        BoneSnapshot initial = bone.getInitialSnapshot();
        if (initial == null) {
            return near(bone.getRotX(), 0.0F) && near(bone.getRotY(), 0.0F) && near(bone.getRotZ(), 0.0F)
                    && near(bone.getPosX(), 0.0F) && near(bone.getPosY(), 0.0F) && near(bone.getPosZ(), 0.0F)
                    && near(bone.getScaleX(), 1.0F) && near(bone.getScaleY(), 1.0F) && near(bone.getScaleZ(), 1.0F);
        }
        return near(bone.getRotX(), initial.getRotX()) && near(bone.getRotY(), initial.getRotY())
                && near(bone.getRotZ(), initial.getRotZ()) && near(bone.getPosX(), initial.getOffsetX())
                && near(bone.getPosY(), initial.getOffsetY()) && near(bone.getPosZ(), initial.getOffsetZ())
                && near(bone.getScaleX(), initial.getScaleX()) && near(bone.getScaleY(), initial.getScaleY())
                && near(bone.getScaleZ(), initial.getScaleZ());
    }

    private static boolean near(float first, float second) {
        return Math.abs(first - second) <= 1.0E-5F;
    }

    private enum GroupState {
        USE,
        HIDDEN,
        FALLBACK
    }
}






