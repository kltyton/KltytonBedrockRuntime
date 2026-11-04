package com.kltyton.bedrockruntime.client.compat.geckolib.layer;

import java.util.function.BiConsumer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.geometry.capture.MeshPartCapture;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.EnhancedBakedGeoModel;
import com.kltyton.bedrockruntime.geometry.mesh.MeshCaptureData;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;

/**
 * CN: 使用 GeckoLib 当前渲染姿态捕获并高亮 cuboid bone。
 * EN: Captures and highlights GeckoLib cuboid bones using their current render pose.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GeckoBoneSelectionLayer extends GeoRenderLayer {
    private static final DataTicket<GeoObbAnimationState> ANIMATION_STATE =
            DataTicket.create("kltyton_bedrock_runtime_collision_animation", GeoObbAnimationState.class);

    public GeckoBoneSelectionLayer(GeoRenderer renderer) {
        super(renderer);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo renderPassInfo, BiConsumer consumer) {
        renderPassInfo.renderState().addGeckolibData(ANIMATION_STATE, animationState(renderPassInfo));
        Object instance = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, -1L);
        if (instance instanceof Number number) {
            Entity entity = entityById(number.longValue());
            if (entity instanceof GeoBoneHitboxEntity geo) {
                var states = com.kltyton.bedrockruntime.client.compat.geckolib.skill.GeoAnimationSnapshots.capture(renderPassInfo);
                geo.kltyton_bedrock_runtime$geoUpdateAnimationStates(states);
                BonePartSelectionClient.recordAnimations(entity.getId(), states);
            }
        }
        for (GeoBone bone : renderPassInfo.model().boneLookup().get().values()) {
            consumer.accept(bone, (PerBoneRender) this::captureBone);
        }
    }

    private void captureBone(RenderPassInfo renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        if (!(bone instanceof CuboidGeoBone cuboidBone)) {
            return;
        }
        MeshCaptureData mesh = renderPassInfo.model() instanceof EnhancedBakedGeoModel enhanced
                ? enhanced.extension().captureData().get(bone.name()) : null;
        if (cuboidBone.cubes.length == 0 && mesh == null) {
            return;
        }

        Object instanceId = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, -1L);
        long entityId = instanceId instanceof Number number ? number.longValue() : -1L;
        if (entityId < 0) {
            return;
        }

        GeoObbAnimationState animationState = (GeoObbAnimationState) renderPassInfo.getOrDefaultGeckolibData(ANIMATION_STATE, GeoObbAnimationState.NONE);
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        bone.translateAwayFromPivotPoint(poseStack);
        for (int i = 0; i < cuboidBone.cubes.length; i++) {
            GeoCube cube = cuboidBone.cubes[i];
            poseStack.pushPose();
            cube.translateToPivotPoint(poseStack);
            cube.rotate(poseStack);
            cube.translateAwayFromPivotPoint(poseStack);

            PartBounds bounds = fromGeckoCube(poseStack.last().pose(), cube);
            if (bounds != null) {
                VisualPartId id = VisualPartId.gecko(entityId, bone.name(), i);
                boolean accepted = BonePartSelectionClient.record(id, bounds, animationState);
                if (!accepted) {
                    poseStack.popPose();
                    continue;
                }
                if (BonePartSelectionClient.shouldRenderOutline(id)) {
                    PartOutlineRenderer.submit(renderTasks, poseStack, bounds);
                }
                if (BonePartSelectionClient.isDamageFlashing(id)) {
                    PartOutlineRenderer.submitDamageFlash(renderTasks, poseStack, bounds);
                }
            }

            poseStack.popPose();
        }
        if (mesh != null && (bone.frameSnapshot == null || !bone.frameSnapshot.isHidden())) {
            MeshPartCapture.capture(entityId, bone.name(), cuboidBone.cubes.length, mesh, poseStack.last().pose(),
                    animationState, (id, bounds) -> {
                        if (BonePartSelectionClient.shouldRenderOutline(id)) {
                            PartOutlineRenderer.submit(renderTasks, poseStack, bounds);
                        }
                        if (BonePartSelectionClient.isDamageFlashing(id)) {
                            PartOutlineRenderer.submitDamageFlash(renderTasks, poseStack, bounds);
                        }
                    });
        }
        poseStack.popPose();
    }

    private static PartBounds fromGeckoCube(org.joml.Matrix4f pose, GeoCube cube) {
        PartBounds.BoundsBuilder builder = PartBounds.builder(pose);
        GeoQuad[] quads = cube.quads();
        if (quads == null) {
            return null;
        }

        for (GeoQuad quad : quads) {
            if (quad == null) {
                continue;
            }

            for (GeoVertex vertex : quad.vertices()) {
                builder.include(vertex.posX(), vertex.posY(), vertex.posZ());
            }
        }
        return builder.build();
    }

    private static GeoObbAnimationState animationState(RenderPassInfo renderPassInfo) {
        Object rawStates = renderPassInfo.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES);
        if (!(rawStates instanceof ControllerState[] states)) {
            return GeoObbAnimationState.NONE;
        }
        if (states == null || states.length == 0) {
            return GeoObbAnimationState.NONE;
        }

        Object rawManager = renderPassInfo.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        AnimatableManager<? extends GeoAnimatable> manager = rawManager instanceof AnimatableManager<?> animatableManager
                ? (AnimatableManager<? extends GeoAnimatable>) animatableManager
                : null;
        for (ControllerState state : states) {
            if (state != null && state.animationPoint() != null) {
                AnimationController<?> controller = controllerForState(manager, state);
                String controllerName = controller == null ? "" : controller.getName();
                boolean triggered = controller != null && controller.isPlayingTriggeredAnimation();
                boolean transitioning = controller != null && controller.isTransitioning();
                boolean finished = controller != null && controller.hasAnimationFinished();
                double timelineTime = controller == null ? 0.0 : controller.getCurrentTimelineTime();
                return new GeoObbAnimationState(
                        controllerName,
                        state.animationPoint().animation().name(),
                        state.animationPoint().animTime(),
                        timelineTime,
                        triggered,
                        transitioning,
                        finished);
            }
        }
        return GeoObbAnimationState.NONE;
    }

    private static AnimationController<?> controllerForState(AnimatableManager<? extends GeoAnimatable> manager, ControllerState state) {
        if (manager == null) {
            return null;
        }

        for (AnimationController<?> controller : manager.getAnimationControllers().values()) {
            if (controller.getCurrentAnimationPoint() == state.animationPoint()) {
                return controller;
            }
        }
        return manager.getAnimationControllers().values().stream().findFirst().orElse(null);
    }

    private static Entity entityById(long entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.getEntity((int) entityId);
    }
}