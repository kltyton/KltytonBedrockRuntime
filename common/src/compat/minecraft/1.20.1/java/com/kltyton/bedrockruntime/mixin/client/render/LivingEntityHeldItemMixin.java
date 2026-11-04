package com.kltyton.bedrockruntime.mixin.client.render;

import java.util.List;
import com.kltyton.bedrockruntime.client.render.item.DiscardingVertexConsumer;
import com.kltyton.bedrockruntime.client.render.item.HeldItemPoseCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityHeldItemMixin<T extends LivingEntity, M extends EntityModel<T>> implements HeldItemPoseCapture {
    @Shadow protected M model;
    @Shadow @Final protected List<RenderLayer<T, M>> layers;
    @Shadow protected abstract void setupRotations(T entity, PoseStack pose, float bob, float bodyRot, float partialTick);
    @Shadow protected abstract void scale(T entity, PoseStack pose, float partialTick);
    @Shadow protected abstract float getBob(T entity, float partialTick);
    @Shadow protected abstract float getAttackAnim(T entity, float partialTick);

    @Override
    @SuppressWarnings("unchecked")
    public void kltyton_bedrock_runtime$captureHeldItems(LivingEntity living, float partialTick, PoseStack pose) {
        T entity = (T) living;
        if (entity instanceof AbstractClientPlayer player && this instanceof PlayerRendererAccessor renderer) {
            renderer.kltyton_bedrock_runtime$setModelProperties(player);
        }
        pose.pushPose();
        try {
            model.attackTime = getAttackAnim(entity, partialTick);
            model.riding = entity.isPassenger();
            model.young = entity.isBaby();
            float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            float headRot = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
            float headYaw = headRot - bodyRot;
            if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
                bodyRot = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
                headYaw = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -85, 85);
                bodyRot = headRot - headYaw;
                if (headYaw * headYaw > 2500) { bodyRot += headYaw * 0.2F; }
                headYaw = headRot - bodyRot;
            }
            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            if (LivingEntityRenderer.isEntityUpsideDown(entity)) { pitch *= -1; headYaw *= -1; }
            headYaw = Mth.wrapDegrees(headYaw);
            if (entity.hasPose(Pose.SLEEPING)) {
                Direction direction = entity.getBedOrientation();
                if (direction != null) {
                    float offset = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                    pose.translate(-direction.getStepX() * offset, 0, -direction.getStepZ() * offset);
                }
            }

            float bob = getBob(entity, partialTick);
            setupRotations(entity, pose, bob, bodyRot, partialTick);
            pose.scale(-1, -1, 1);
            scale(entity, pose, partialTick);
            pose.translate(0, -1.501F, 0);
            float limbSpeed = 0;
            float limbPosition = 0;
            if (!entity.isPassenger() && entity.isAlive()) {
                limbSpeed = Math.min(entity.walkAnimation.speed(partialTick), 1);
                limbPosition = entity.walkAnimation.position(partialTick) * (entity.isBaby() ? 3 : 1);
            }
            model.prepareMobModel(entity, limbPosition, limbSpeed, partialTick);
            model.setupAnim(entity, limbPosition, limbSpeed, bob, headYaw, pitch);
            if (!entity.isSpectator()) {
                for (RenderLayer<T, M> layer : layers) {
                    if (layer instanceof ItemInHandLayer<?, ?>) {
                        layer.render(pose, type -> DiscardingVertexConsumer.INSTANCE, 15728880, entity,
                                limbPosition, limbSpeed, partialTick, bob, headYaw, pitch);
                    }
                }
            }
        } finally { pose.popPose(); }
    }
}
