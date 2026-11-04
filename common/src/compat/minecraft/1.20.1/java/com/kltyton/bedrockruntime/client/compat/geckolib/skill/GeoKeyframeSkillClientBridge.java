package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import com.kltyton.bedrockruntime.api.geckolib.skill.entity.GeoKeyframeSkillEntity;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.client.skill.keyframe.GeoKeyframeSkillClientQueue;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import net.minecraft.world.entity.Entity;

/** Forward an entity's custom instruction handler to the bounded server-side skill queue. */
public final class GeoKeyframeSkillClientBridge {
    private GeoKeyframeSkillClientBridge() { }

    public static <T extends GeoAnimatable> void forward(CustomInstructionKeyframeEvent<T> event) {
        if (!(event.getAnimatable() instanceof Entity entity)
                || !(entity instanceof GeoKeyframeSkillEntity) || !entity.level().isClientSide) { return; }
        var controller = event.getController();
        GeoObbAnimationState clock = GeoAnimationSnapshots.capture(controller);
        GeoObbAnimationState state = new GeoObbAnimationState(clock.controllerName(), clock.animationName(),
                event.getAnimationTick() / 20.0, clock.timelineTimeSeconds(), clock.triggered(),
                clock.transitioning(), clock.finished());
        GeoKeyframeSkillClientQueue.enqueue(new GeoKeyframeSkillPayload(entity.getId(),
                GeoKeyframeSkillClientQueue.nextSequence(), entity.level().getGameTime(),
                event.getKeyframeData().getInstructions(), state, event.getKeyframeData().getStartTick() / 20.0,
                controller.getAnimationSpeed()));
    }
}
