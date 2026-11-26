package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.ProjectileHitSweep;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the collision ray that selected the arrow's target on Minecraft 1.20.1 and 1.21.1. */
@Mixin(AbstractArrow.class)
public abstract class ArrowHitSweepMixin {
    @Inject(method = "findHitEntity", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$captureHitSweep(Vec3 from, Vec3 to,
            CallbackInfoReturnable<EntityHitResult> callback) {
        EntityHitResult hit = callback.getReturnValue();
        if (hit != null && !((AbstractArrow) (Object) this).level().isClientSide()) {
            ProjectileHitSweep.capture(hit, from, to);
        }
    }
}
