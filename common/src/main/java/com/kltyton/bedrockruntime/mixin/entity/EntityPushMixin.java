package com.kltyton.bedrockruntime.mixin.entity;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CN: 任一实体使用 OBB 物理碰撞时，取消该实体对的原版 AABB 推挤，避免两套响应叠加。
 * EN: Cancels vanilla AABB pushing for a pair when either entity uses OBB physics, preventing duplicate responses.
 */
@Mixin(Entity.class)
public abstract class EntityPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$replaceVanillaEntityPush(Entity other, CallbackInfo callbackInfo) {
        Entity self = (Entity) (Object) this;
        if (kltyton_bedrock_runtime$usesObbPhysics(self) || kltyton_bedrock_runtime$usesObbPhysics(other)) {
            callbackInfo.cancel();
        }
    }

    private static boolean kltyton_bedrock_runtime$usesObbPhysics(Entity entity) {
        return entity instanceof BoneHitboxEntity hitboxEntity
                && hitboxEntity.kltyton_bedrock_runtime$obbState().replacesVanillaEntityPush();
    }
}
