package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.ProjectileHitSweep;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the vanilla collision ray on entity hit results produced via ProjectileUtil for all projectiles. */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilHitSweepMixin {
    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN")
    )
    private static void kltyton_bedrock_runtime$captureProjectileHitSweep(
            Level level,
            Entity projectile,
            Vec3 from,
            Vec3 to,
            AABB bounds,
            Predicate<Entity> filter,
            float margin,
            CallbackInfoReturnable<EntityHitResult> callback) {
        EntityHitResult hit = callback.getReturnValue();
        if (hit != null && !level.isClientSide() && projectile instanceof Projectile) {
            ProjectileHitSweep.capture(hit, from, to);
        }
    }
}
