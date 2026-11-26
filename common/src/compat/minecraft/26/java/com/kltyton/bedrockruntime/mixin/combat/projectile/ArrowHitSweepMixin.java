package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.ProjectileHitSweep;
import java.util.Collection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the collision ray before the arrow advances to the first vanilla hit location. */
@Mixin(AbstractArrow.class)
public abstract class ArrowHitSweepMixin {
    @Inject(method = "findHitEntities", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$captureHitSweeps(Vec3 from, Vec3 to,
            CallbackInfoReturnable<Collection<EntityHitResult>> callback) {
        if (!((AbstractArrow) (Object) this).level().isClientSide()) {
            for (EntityHitResult hit : callback.getReturnValue()) {
                ProjectileHitSweep.capture(hit, from, to);
            }
        }
    }
}
