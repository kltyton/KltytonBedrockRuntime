package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import com.kltyton.bedrockruntime.api.geckolib.skill.entity.GeoKeyframeSkillEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.skill.keyframe.GeoKeyframeSkillClientQueue;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;

import net.minecraft.world.entity.Entity;

/**
 * CN: 可选 GeckoLib 客户端桥。用户在 custom instruction handler 中调用 {@link #forward(KeyFrameEvent)}；
 * 可见渲染与库的逻辑 tick 提取都会经过同一 handler。
 * EN: Optional GeckoLib client bridge. Call {@link #forward(KeyFrameEvent)} from a custom instruction handler;
 * both visible rendering and the library's logical tick extraction pass through that same handler.
 */
public final class GeoKeyframeSkillClientBridge {
    private GeoKeyframeSkillClientBridge() {
    }

    public static <T extends GeoAnimatable> void forward(
            KeyFrameEvent<T, CustomInstructionKeyframeData> event) {
        if (!(event.animatable() instanceof Entity entity)
                || !(entity instanceof GeoKeyframeSkillEntity)
                || !entity.level().isClientSide()) {
            return;
        }

        var controller = event.controller();
        var animationPoint = controller.getCurrentAnimationPoint();
        String animationName = animationPoint == null ? "" : animationPoint.animation().name();
        GeoObbAnimationState animationState = new GeoObbAnimationState(
                controller.getName(),
                animationName,
                controller.getCurrentAnimationTime(),
                controller.getCurrentTimelineTime(),
                controller.isPlayingTriggeredAnimation(),
                controller.isTransitioning(),
                controller.hasAnimationFinished());
        GeoKeyframeSkillClientQueue.enqueue(new GeoKeyframeSkillPayload(
                entity.getId(),
                GeoKeyframeSkillClientQueue.nextSequence(),
                entity.level().getGameTime(),
                event.keyframeData().getInstructions(),
                animationState,
                event.keyframeData().getTime(),
                controller.getAnimationSpeed()));
    }
}
