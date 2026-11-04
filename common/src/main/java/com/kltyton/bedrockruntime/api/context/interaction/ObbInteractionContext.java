package com.kltyton.bedrockruntime.api.context.interaction;

import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * CN: 右键交互 OBB 部位时的服务端上下文。
 * EN: Server-side context for right-clicking an OBB part.
 */
public record ObbInteractionContext(Player player, Entity target, InteractionHand hand, ObbBoneState bone,
        GeoObbAnimationState targetAnimationState) {
    public ObbInteractionContext(Player player, Entity target, InteractionHand hand, ObbBoneState bone) {
        this(player, target, hand, bone, GeoObbAnimationState.NONE);
    }

    public ObbInteractionContext {
        targetAnimationState = targetAnimationState == null ? GeoObbAnimationState.NONE : targetAnimationState;
    }
}
