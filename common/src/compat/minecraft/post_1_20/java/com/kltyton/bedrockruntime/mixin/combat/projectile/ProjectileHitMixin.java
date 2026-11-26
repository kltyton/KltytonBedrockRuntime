package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.BoneHitboxProjectileHooks;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore.AttackContact;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Correlates the actual projectile hit with part damage while preserving the vanilla call. */
@Mixin(Projectile.class)
public abstract class ProjectileHitMixin {
    @WrapOperation(method = "hitTargetOrDeflectSelf", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void kltyton_bedrock_runtime$onVanillaProjectileHit(Projectile projectile, HitResult hit,
            Operation<Void> original) {
        if (!(hit instanceof EntityHitResult entityHit)) {
            original.call(projectile, hit);
            return;
        }
        AttackContact contact = BoneHitboxProjectileHooks.resolveArrowHit(projectile, entityHit);
        original.call(projectile, hit);
        BoneHitboxProjectileHooks.afterHit(projectile, entityHit, contact);
    }
}
