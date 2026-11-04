package com.kltyton.bedrockruntime.client.render.capture;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Samples nearby culled models without sending their deferred draw tasks to the renderer. */
public final class CulledEntityCapture {
    private static final double CAPTURE_RANGE_SQR = 32.0 * 32.0;

    private CulledEntityCapture() { }

    public static AABB extendCullingBounds(AABB original, Entity entity, Frustum frustum,
            double cameraX, double cameraY, double cameraZ) {
        if (!(entity instanceof BoneHitboxEntity)
                || entity.distanceToSqr(cameraX, cameraY, cameraZ) > CAPTURE_RANGE_SQR
                || original.hasNaN() || original.getSize() == 0.0 || frustum.isVisible(original)) {
            return original;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return original;
        }
        AABB modelBounds = ModelBoundsCapture.getOrSample(level, entity,
                () -> sample(minecraft, entity, new Vec3(cameraX, cameraY, cameraZ)));
        return modelBounds == null ? original : original.minmax(modelBounds.inflate(0.5));
    }

    public static void record(VisualPartId part, AABB renderBounds) {
        ModelBoundsCapture.record(part, renderBounds);
    }

    public static void clear() {
        ModelBoundsCapture.clear();
    }

    private static AABB sample(Minecraft minecraft, Entity entity, Vec3 camera) {
        var dispatcher = minecraft.getEntityRenderDispatcher();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var state = dispatcher.extractEntity(entity, partialTick);
        return ModelBoundsCapture.capture(entity, camera, () -> {
            // Native collectors defer drawing; model/layer callbacks still produce the real collision geometry.
            dispatcher.submit(state, minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState,
                    state.x - camera.x, state.y - camera.y, state.z - camera.z,
                    new PoseStack(), new SubmitNodeStorage());
        });
    }
}
