package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

import java.util.ArrayList;
import java.util.List;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;

/** Copies all controller clocks while already on the client thread; no live GeckoLib objects cross the network. */
public final class GeoAnimationSnapshots {
    private GeoAnimationSnapshots() { }

    public static List<GeoObbAnimationState> capture(Object rawState) {
        if (!(rawState instanceof GeoRenderState state)) { return List.of(); }
        AnimatableManager<?> manager = state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) { return List.of(); }
        List<GeoObbAnimationState> snapshots = new ArrayList<>();
        for (var controller : manager.getAnimationControllers().values()) {
            var point = controller.getCurrentAnimationPoint();
            snapshots.add(new GeoObbAnimationState(controller.getName(),
                    point == null ? "" : point.animation().name(),
                    controller.getCurrentAnimationTime(), controller.getCurrentTimelineTime(),
                    controller.isPlayingTriggeredAnimation(), controller.isTransitioning(), controller.hasAnimationFinished()));
        }
        return List.copyOf(snapshots);
    }
}
