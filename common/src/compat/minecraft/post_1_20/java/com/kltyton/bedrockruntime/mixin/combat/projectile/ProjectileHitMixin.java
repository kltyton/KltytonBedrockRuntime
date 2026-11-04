package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 将原版投射物实体命中桥接到 OBB 命中回调。
 * EN: Bridges vanilla projectile entity hits into OBB hit callbacks.
 */
@Mixin(Projectile.class)
public abstract class ProjectileHitMixin {
    @Inject(
            method = "hitTargetOrDeflectSelf",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V",
                    shift = At.Shift.AFTER))
    private void kltyton_bedrock_runtime$afterVanillaProjectileHit(HitResult hitResult,
            CallbackInfoReturnable<ProjectileDeflection> callbackInfo) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            BoneHitboxServerHooks.handleProjectileHit((Projectile) (Object) this, entityHitResult.getEntity());
        }
    }
}
