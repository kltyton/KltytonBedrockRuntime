package com.kltyton.bedrockruntime.mixin.combat;

import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CN: 在 Player.attack 完成后派发原版攻击部位事件，使伤害上下文包含实际结果。
 * EN: Dispatches vanilla part-attack events after Player.attack so the context contains actual damage results.
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$afterPlayerAttack(Entity target, CallbackInfo callbackInfo) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            BoneHitboxServerHooks.handlePlayerAttack(player, target);
        }
    }
}
