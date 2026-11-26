package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.BoneHitboxProjectileHooks;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore.AttackContact;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Correlates the actual projectile hit with part damage while preserving the vanilla call. */
@Mixin(Projectile.class)
public abstract class ProjectileHitMixin {
    @WrapOperation(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V"))
    private void kltyton_bedrock_runtime$onVanillaProjectileHit(Projectile projectile, EntityHitResult hit,
            Operation<Void> original) {
        AttackContact contact = BoneHitboxProjectileHooks.resolveArrowHit(projectile, hit);
        original.call(projectile, hit);
        BoneHitboxProjectileHooks.afterHit(projectile, hit, contact);
    }
}
