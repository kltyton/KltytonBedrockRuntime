package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Advances the same model/controller path as a visible render without issuing draw calls. */
public final class GeoAnimationTicker {
    private GeoAnimationTicker() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void tick(Minecraft minecraft, Entity entity) {
        if (!(entity instanceof GeoAnimatable animatable)
                || !(minecraft.getEntityRenderDispatcher().getRenderer(entity) instanceof GeoEntityRenderer renderer)) { return; }
        var model = renderer.getGeoModel();
        model.getBakedModel(model.getModelResource(animatable, renderer));
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        boolean sitting = entity.isPassenger();
        float amount = living == null || sitting || !entity.isAlive() ? 0 : Math.min(1, living.walkAnimation.speed(1));
        float swing = living == null ? 0 : living.walkAnimation.position(1) * (living.isBaby() ? 3 : 1);
        var velocity = entity.getDeltaMovement();
        AnimationState state = new AnimationState(animatable, swing, amount, 1,
                (Math.abs(velocity.x) + Math.abs(velocity.z)) / 2 >= renderer.getMotionAnimThreshold(animatable) && amount != 0);
        state.setData(DataTickets.TICK, animatable.getTick(entity));
        state.setData(DataTickets.ENTITY, entity);
        float headYaw = living == null ? 0 : living.yHeadRot - living.yBodyRot;
        state.setData(DataTickets.ENTITY_MODEL_DATA,
                new EntityModelData(sitting, living != null && living.isBaby(), -headYaw, -entity.getXRot()));
        long instanceId = renderer.getInstanceId(animatable);
        model.addAdditionalStateData(animatable, instanceId, (ticket, value) -> state.setData((software.bernie.geckolib.core.object.DataTicket) ticket, value));
        model.handleAnimations(animatable, instanceId, state);
    }
}
