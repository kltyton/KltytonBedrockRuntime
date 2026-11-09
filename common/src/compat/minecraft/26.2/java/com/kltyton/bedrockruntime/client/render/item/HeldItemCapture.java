package com.kltyton.bedrockruntime.client.render.item;

import com.kltyton.bedrockruntime.api.bone.builtin.ObbBuiltinBones;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.render.capture.EntityCapturePose;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;
import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class HeldItemCapture {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<SubmitNodeCollector> LOCAL_OUTLINE_OUTPUT = new ThreadLocal<>();

    private HeldItemCapture() { }

    public static void submit(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector output,
            int light, int overlay, int outline, ArmedEntityRenderState state, HumanoidArm arm) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(state, arm));
        try { item.submit(pose, output, light, overlay, outline); }
        finally {
            if (previous == null) { ACTIVE.remove(); } else { ACTIVE.set(previous); }
        }
    }

    public static void layer(PoseStack pose, SubmitNodeCollector output, Vector3fc[] extents) {
        Context context = ACTIVE.get();
        if (context == null) { return; }
        int index = context.layer++;
        PartBounds.BoundsBuilder builder = PartBounds.builder(pose.last().pose());
        for (Vector3fc point : extents) { builder.include(point.x(), point.y(), point.z()); }
        PartBounds bounds = builder.build();
        if (bounds == null) { return; }
        VisualPartId id = new VisualPartId(ObbBuiltinBones.HELD_ITEM_SOURCE, context.owner,
                context.arm == HumanoidArm.LEFT ? ObbBuiltinBones.LEFT_HELD_ITEM_NAME : ObbBuiltinBones.RIGHT_HELD_ITEM_NAME,
                index);
        if (BonePartSelectionClient.record(id, bounds, GeoObbAnimationState.NONE, context.capturePose)
                && BonePartSelectionClient.shouldRenderOutline(id)) {
            SubmitNodeCollector localOutput = LOCAL_OUTLINE_OUTPUT.get();
            PartOutlineRenderer.submit(localOutput == null ? output : localOutput, pose, bounds);
        }
    }

    public static void captureLocalPlayer(Minecraft minecraft) {
        captureLocalPlayer(minecraft, minecraft.gameRenderer.mainCamera().position(), 1.0F);
    }

    public static void submitLocalPlayerOutlines(Minecraft minecraft, Vec3 camera, SubmitNodeCollector output) {
        if (!BoneHitboxClientOptions.shouldRenderDebugObbs(minecraft)
                && !BoneHitboxClientOptions.shouldRenderCrosshairObb(minecraft)) { return; }
        SubmitNodeCollector previous = LOCAL_OUTLINE_OUTPUT.get();
        LOCAL_OUTLINE_OUTPUT.set(output);
        try {
            captureLocalPlayer(minecraft, camera, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        } finally {
            if (previous == null) { LOCAL_OUTLINE_OUTPUT.remove(); } else { LOCAL_OUTLINE_OUTPUT.set(previous); }
        }
    }

    private static void captureLocalPlayer(Minecraft minecraft, Vec3 camera, float partialTick) {
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) { return; }
        var dispatcher = minecraft.getEntityRenderDispatcher();
        var state = dispatcher.extractEntity(minecraft.player, partialTick);
        if (state instanceof LivingEntityRenderState living
                && dispatcher.getRenderer(minecraft.player) instanceof HeldItemPoseCapture capture) {
            BonePartSelectionClient.clearHeldItemCaptures(minecraft.player.getId());
            var offset = dispatcher.getRenderer(living).getRenderOffset(living);
            PoseStack pose = new PoseStack();
            pose.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            capture.kltyton_bedrock_runtime$captureHeldItems(living, pose);
        }
    }

    private static final class Context {
        private final String owner;
        private final HumanoidArm arm;
        private final EntityCapturePose capturePose;
        private int layer;
        private Context(ArmedEntityRenderState state, HumanoidArm arm) {
            this.owner = VisualPartId.ownerKey(state);
            this.arm = arm;
            this.capturePose = state instanceof VisualPartEntityRenderState source
                    ? source.kltyton_bedrock_runtime$getCapturePose() : null;
        }
    }
}
