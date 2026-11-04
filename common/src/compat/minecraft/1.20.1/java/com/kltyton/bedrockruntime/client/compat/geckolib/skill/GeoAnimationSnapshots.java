package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

import java.util.ArrayList;
import java.util.List;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import net.minecraft.world.entity.Entity;

/** Converts GeckoLib 4 tick clocks to the library's seconds-based wire contract. */
public final class GeoAnimationSnapshots {
    private GeoAnimationSnapshots() { }

    public static List<GeoObbAnimationState> capture(Entity entity) {
        if (!(entity instanceof GeoAnimatable animatable)) { return List.of(); }
        var manager = animatable.getAnimatableInstanceCache().getManagerForId(entity.getId());
        List<GeoObbAnimationState> snapshots = new ArrayList<>();
        for (var controller : manager.getAnimationControllers().values()) { snapshots.add(capture(controller)); }
        return List.copyOf(snapshots);
    }

    public static GeoObbAnimationState capture(AnimationController<?> controller) {
        var animation = controller.getCurrentAnimation();
        GeoControllerClock clock = (GeoControllerClock) controller;
        boolean transitioning = controller.getAnimationState() == AnimationController.State.TRANSITIONING;
        double animationTick = transitioning ? 0 : Math.max(0,
                clock.kltyton_bedrock_runtime$getTimelineTick() - clock.kltyton_bedrock_runtime$getTickOffset()) * controller.getAnimationSpeed();
        if (animation != null && (controller.getAnimationState() == AnimationController.State.PAUSED
                || controller.hasAnimationFinished())) {
            animationTick = Math.min(animationTick, animation.animation().length());
        }
        return new GeoObbAnimationState(controller.getName(), animation == null ? "" : animation.animation().name(),
                animationTick / 20.0, Math.max(0, clock.kltyton_bedrock_runtime$getTimelineTick()) / 20.0,
                controller.isPlayingTriggeredAnimation(), transitioning, controller.hasAnimationFinished());
    }
}
