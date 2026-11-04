package com.kltyton.bedrockruntime.examples;

import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ExampleParameters {
    private ExampleParameters() {
    }

    public static void capture(ExampleScene scene, GraphRuntime runtime, Object relatedObject, Object localPlayer,
            float partialTick) {
        ExampleTelemetry.capture(scene.id());
        if (!(localPlayer instanceof Entity player)) {
            runtime.clearParameters();
            return;
        }
        switch (scene) {
            case SPEED_1D -> runtime.setParameter("speed", Math.min(2.0, horizontalSpeed(player) * 12.0));
            case AIM_2D -> {
                float yaw = 0.0f;
                float pitch = 0.0f;
                if (player instanceof LivingEntity living) {
                    yaw = Mth.wrapDegrees(living.yHeadRot - living.yBodyRot);
                    pitch = living.getXRot();
                }
                runtime.setParameter("yaw", clamp(yaw / 90.0));
                runtime.setParameter("pitch", clamp(pitch / 90.0));
            }
            case LEGS_AIM -> runtime.setParameter("aim", player.isCrouching() ? 0.25 : 1.0);
            case ADDITIVE -> runtime.setParameter("gesture", player.isCrouching() ? 0.15 : 0.75);
            case HERMITE -> runtime.setParameter("phase", (player.tickCount % 60) / 60.0);
            default -> {
            }
        }
    }

    private static double horizontalSpeed(Entity entity) {
        var velocity = entity.getDeltaMovement();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }
}
