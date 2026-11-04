package com.kltyton.bedrockruntime.mixin.combat;

import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 在原版 Mob 攻击成功后关联客户端攻击骨骼，不替代或重复调用原版伤害逻辑。
 * EN: Correlates client attack bones after a vanilla Mob attack succeeds without replacing or replaying vanilla damage logic.
 */
@Mixin(Mob.class)
public abstract class MobAttackMixin {
    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$afterVanillaMobAttack(ServerLevel level, Entity target,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (callbackInfo.getReturnValue()) {
            BoneHitboxServerHooks.handleEntityAttack((Mob) (Object) this, target);
        }
    }
}
