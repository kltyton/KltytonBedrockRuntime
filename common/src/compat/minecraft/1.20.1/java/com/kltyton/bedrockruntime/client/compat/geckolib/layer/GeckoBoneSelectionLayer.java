package com.kltyton.bedrockruntime.client.compat.geckolib.layer;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;
import com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.compat.geckolib.skill.GeoAnimationSnapshots;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.geometry.capture.MeshPartCapture;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

/** Captures actual animated cube transforms from GeckoLib's per-bone render callback. */
public final class GeckoBoneSelectionLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public GeckoBoneSelectionLayer(GeoRenderer<T> renderer) { super(renderer); }

    @Override
    public void render(PoseStack pose, T animatable, BakedGeoModel model, RenderType type,
            MultiBufferSource output, VertexConsumer vertices, float partialTick, int light, int overlay) {
        Entity entity = entity(animatable);
        if (entity instanceof GeoBoneHitboxEntity geo) {
            var states = GeoAnimationSnapshots.capture(entity);
            geo.kltyton_bedrock_runtime$geoUpdateAnimationStates(states);
            BonePartSelectionClient.recordAnimations(entity.getId(), states);
        }
    }

    @Override
    public void renderForBone(PoseStack pose, T animatable, GeoBone bone, RenderType type,
            MultiBufferSource output, VertexConsumer vertices, float partialTick, int light, int overlay) {
        Entity entity = entity(animatable);
        if (entity == null || bone.isHidden()) { return; }
        var states = GeoAnimationSnapshots.capture(entity);
        GeoObbAnimationState animation = states.stream().filter(GeoObbAnimationState::active).findFirst().orElse(GeoObbAnimationState.NONE);
        for (int index = 0; index < bone.getCubes().size(); index++) {
            GeoCube cube = bone.getCubes().get(index);
            if (cube.quads() == null) { continue; }
            pose.pushPose();
            RenderUtils.translateToPivotPoint(pose, cube);
            RenderUtils.rotateMatrixAroundCube(pose, cube);
            RenderUtils.translateAwayFromPivotPoint(pose, cube);
            PartBounds.BoundsBuilder builder = PartBounds.builder(pose.last().pose());
            for (var quad : cube.quads()) {
                if (quad == null) { continue; }
                for (var vertex : quad.vertices()) {
                    var pos = vertex.position();
                    builder.include(pos.x(), pos.y(), pos.z());
                }
            }
            PartBounds bounds = builder.build();
            if (bounds != null) {
                VisualPartId id = VisualPartId.gecko(entity.getId(), bone.getName(), index);
                if (BonePartSelectionClient.record(id, bounds, animation)) {
                    if (BonePartSelectionClient.shouldRenderOutline(id)) { PartOutlineRenderer.submit(output, pose, bounds); }
                    if (BonePartSelectionClient.isDamageFlashing(id)) { PartOutlineRenderer.submitDamageFlash(output, pose, bounds); }
                }
            }
            pose.popPose();
        }
        var mesh = MeshGeoBone.captureData(bone);
        if (mesh != null) {
            MeshPartCapture.capture(entity.getId(), bone.getName(), bone.getCubes().size(), mesh,
                    pose.last().pose(), animation, (id, bounds) -> {
                        if (BonePartSelectionClient.shouldRenderOutline(id)) { PartOutlineRenderer.submit(output, pose, bounds); }
                        if (BonePartSelectionClient.isDamageFlashing(id)) { PartOutlineRenderer.submitDamageFlash(output, pose, bounds); }
                    });
        }
    }

    private Entity entity(T animatable) {
        if (animatable instanceof Entity entity) { return entity; }
        return getRenderer() instanceof GeoReplacedEntityRenderer<?, ?> replaced ? replaced.getCurrentEntity() : null;
    }
}
