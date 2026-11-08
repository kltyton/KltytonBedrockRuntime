package com.kltyton.bedrockruntime.client.render.item;

import java.util.List;
import com.kltyton.bedrockruntime.api.bone.builtin.ObbBuiltinBones;
import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import com.kltyton.bedrockruntime.client.render.vanilla.ModelPartObservations;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HeldItemCapture {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static boolean captureOnly;

    private HeldItemCapture() { }

    public static void render(LivingEntity entity, HumanoidArm arm, Runnable draw) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(VisualPartId.ownerKey(entity), arm));
        try { draw.run(); }
        finally { if (previous == null) { ACTIVE.remove(); } else { ACTIVE.set(previous); } }
    }

    public static void model(PoseStack.Pose pose, BakedModel model) {
        if (ACTIVE.get() == null) { return; }
        PartBounds.BoundsBuilder builder = PartBounds.builder(pose.pose());
        RandomSource random = RandomSource.create();
        for (int face = 0; face <= Direction.values().length; face++) {
            random.setSeed(42);
            Direction direction = face == Direction.values().length ? null : Direction.values()[face];
            for (var quad : model.getQuads(null, direction, random)) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    builder.include(Float.intBitsToFloat(vertices[offset]), Float.intBitsToFloat(vertices[offset + 1]),
                            Float.intBitsToFloat(vertices[offset + 2]));
                }
            }
        }
        record(pose, builder.build());
    }

    public static void modelPart(PoseStack.Pose pose, List<ModelPart.Cube> cubes) {
        Context context = ACTIVE.get();
        if (context == null || context.observations.matchesLast("", pose.pose(), cubes)) { return; }
        recordCubes(pose, cubes);
    }

    private static void recordCubes(PoseStack.Pose pose, List<ModelPart.Cube> cubes) {
        for (ModelPart.Cube cube : cubes) {
            record(pose, PartBounds.fromVanillaCube(pose.pose(), cube));
        }
    }

    public static void modelPart(ModelPart part, PoseStack.Pose pose, List<ModelPart.Cube> cubes) {
        Context context = ACTIVE.get();
        if (context == null || !context.observations.add(part, "", pose.pose(), cubes)) { return; }
        recordCubes(pose, cubes);
    }

    public static boolean isActive() { return ACTIVE.get() != null; }

    private static void record(PoseStack.Pose pose, PartBounds bounds) {
        Context context = ACTIVE.get();
        if (bounds == null || context == null) { return; }
        VisualPartId id = new VisualPartId(ObbBuiltinBones.HELD_ITEM_SOURCE, context.owner,
                context.arm == HumanoidArm.LEFT ? ObbBuiltinBones.LEFT_HELD_ITEM_NAME : ObbBuiltinBones.RIGHT_HELD_ITEM_NAME,
                context.layer++);
        if (BonePartSelectionClient.record(id, bounds) && !captureOnly && BonePartSelectionClient.shouldRenderOutline(id)) {
            PartOutlineRenderer.submit(pose, bounds, false);
        }
    }

    public static void captureLocalPlayer(Minecraft minecraft) {
        captureLocalPlayer(minecraft, 1.0F, true);
    }

    public static void submitLocalPlayerOutlines(Minecraft minecraft) {
        if (BoneHitboxClientOptions.shouldRenderDebugObbs(minecraft)) {
            captureLocalPlayer(minecraft, minecraft.getTimer().getGameTimeDeltaPartialTick(false), false);
        }
    }

    private static void captureLocalPlayer(Minecraft minecraft, float partialTick, boolean onlyCapture) {
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) { return; }
        var player = minecraft.player;
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof HeldItemPoseCapture capture)) { return; }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 offset = renderer.getRenderOffset(player, partialTick);
        PoseStack pose = new PoseStack();
        pose.translate(Mth.lerp(partialTick, player.xOld, player.getX()) - camera.x + offset.x,
                Mth.lerp(partialTick, player.yOld, player.getY()) - camera.y + offset.y,
                Mth.lerp(partialTick, player.zOld, player.getZ()) - camera.z + offset.z);
        boolean previous = captureOnly;
        captureOnly = onlyCapture;
        try { capture.kltyton_bedrock_runtime$captureHeldItems(player, partialTick, pose); }
        finally { captureOnly = previous; }
    }

    private static final class Context {
        private final String owner;
        private final HumanoidArm arm;
        private final ModelPartObservations observations = new ModelPartObservations();
        private int layer;
        private Context(String owner, HumanoidArm arm) { this.owner = owner; this.arm = arm; }
    }
}
