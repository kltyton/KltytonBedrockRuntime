package com.kltyton.bedrockruntime.mixin.entity;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps vanilla pushing for players without registered parts and avoids duplicate pushing for OBB contacts.
 */
@Mixin(Entity.class)
public abstract class EntityPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void kltyton_bedrock_runtime$replaceVanillaEntityPush(Entity other, CallbackInfo callbackInfo) {
        Entity self = (Entity) (Object) this;
        if ((self instanceof Player && !(self instanceof BoneHitboxEntity))
                || (other instanceof Player && !(other instanceof BoneHitboxEntity))) return;
        if (kltyton_bedrock_runtime$usesObbPhysics(self) || kltyton_bedrock_runtime$usesObbPhysics(other)) {
            callbackInfo.cancel();
        }
    }

    private static boolean kltyton_bedrock_runtime$usesObbPhysics(Entity entity) {
        return entity instanceof BoneHitboxEntity hitboxEntity
                && hitboxEntity.kltyton_bedrock_runtime$obbState().replacesVanillaEntityPush();
    }
}
