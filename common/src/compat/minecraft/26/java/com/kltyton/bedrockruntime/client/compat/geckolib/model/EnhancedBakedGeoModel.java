package com.kltyton.bedrockruntime.client.compat.geckolib.model;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GL5CompiledGeometry;
import com.kltyton.bedrockruntime.client.model.mesh.MeshVertexEmitter;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.model.optimization.CompiledModelPlan;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adds mesh geometry while retaining GeckoLib's original bones, cubes and locator identities. */
public final class EnhancedBakedGeoModel extends BakedGeoModel {
    private final EnhancedModelData extension;
    private final SkeletonBinding skeleton;
    private final CompiledModelPlan compiledPlan;

    public EnhancedBakedGeoModel(BakedGeoModel original, EnhancedModelData extension) {
        super(original.topLevelBones(), original.locators(), original.properties(), original.boneLookup());
        this.extension = extension;
        List<SkeletonBinding.Bone> bones = new ArrayList<>();
        for (GeoBone root : topLevelBones()) {
            bind(root, -1, bones);
        }
        this.skeleton = new SkeletonBinding(extension.resource(), extension.generation(), bones);
        EnhancedBoneMeshes.attach(this, extension);
        this.compiledPlan = extension.mode() == EnhancedModelData.Mode.BAKED
                ? GL5CompiledGeometry.compile(extension, this, EnhancedModels.consumers(extension.resource())) : null;
    }

    public EnhancedModelData extension() { return extension; }
    public SkeletonBinding skeleton() { return skeleton; }
    public TriangleMesh mesh(String bone) { return extension.meshes().get(bone); }

    @Override
    public <R extends GeoRenderState> void render(RenderPassInfo<R> pass, VertexConsumer consumer,
                                                 int light, int overlay, int color) {
        if (compiledPlan != null && compiledPlan.compiled()) {
            GL5CompiledGeometry.render(compiledPlan, pass, consumer, light, overlay, color);
            return;
        }
        super.render(pass, consumer, light, overlay, color);
        PoseStack poses = pass.poseStack();
        for (Map.Entry<String, TriangleMesh> entry : extension.meshes().entrySet()) {
            GeoBone bone = getBone(entry.getKey()).orElse(null);
            if (bone == null || bone instanceof CuboidGeoBone || !visible(bone)) {
                continue;
            }
            poses.pushPose();
            try {
                RenderUtil.transformToBone(poses, bone);
                bone.translateAwayFromPivotPoint(poses);
                MeshVertexEmitter.emit(entry.getValue(), poses.last().pose(), poses.last().normal(), color,
                        light, overlay, extension.doubleSided(), consumer::addVertex);
            } finally {
                poses.popPose();
            }
        }
    }

    private static void bind(GeoBone bone, int parent, List<SkeletonBinding.Bone> bones) {
        int index = bones.size();
        bones.add(new SkeletonBinding.Bone(bone.name(), parent < 0 ? null : bones.get(parent).name(), bone.pivotX(), bone.pivotY(), bone.pivotZ(),
                bone.baseRotX(), bone.baseRotY(), bone.baseRotZ()));
        for (GeoBone child : bone.children()) {
            bind(child, index, bones);
        }
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
}
