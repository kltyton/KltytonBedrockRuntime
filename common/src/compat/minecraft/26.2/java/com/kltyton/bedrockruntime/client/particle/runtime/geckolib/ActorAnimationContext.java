package com.kltyton.bedrockruntime.client.particle.runtime.geckolib;

import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.Keyframe;
import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;

/** Carries the actor identity into bone sampling and records its current channel ratio. */
public final class ActorAnimationContext {
    public static final DataTicket<Entity> ENTITY = DataTicket.create(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID + ":animation_entity", Entity.class);

    private ActorAnimationContext() {
    }

    public static void recordKeyframeLerp(ControllerState state, AnimationPoint animation, int boneIndex,
            AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        Entity entity = state.renderState().getGeckolibData(ENTITY);
        if (entity == null) return;
        Keyframe from = animation.getCurrentKeyframe(boneIndex, transform, axis);
        Keyframe to = animation.getNextKeyframe(boneIndex, transform, axis);
        if (from == null || to == null) return;
        if (from == to) {
            from = animation.getPreviousKeyframe(boneIndex, transform, axis);
            if (from == to) return;
        }
        float end = (float) to.startTime();
        float interval = end - (float) from.startTime();
        float ratio = interval >= 1.0E-6F ? 1.0F - (end - (float) animation.animTime()) / interval : 1.0F;
        IEntity.of(entity).kltyton_bedrock_runtime$setKeyframeLerpTime(ratio);
    }
}
