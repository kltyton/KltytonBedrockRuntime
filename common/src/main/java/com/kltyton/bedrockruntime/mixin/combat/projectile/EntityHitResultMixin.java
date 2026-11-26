package com.kltyton.bedrockruntime.mixin.combat.projectile;

import com.kltyton.bedrockruntime.server.combat.projectile.ProjectileHitSweep;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Retains the collision segment on the specific vanilla entity hit, including piercing hits. */
@Mixin(EntityHitResult.class)
public abstract class EntityHitResultMixin implements ProjectileHitSweep {
    @Unique
    private @Nullable Segment kltyton_bedrock_runtime$sweep;

    @Override
    public @Nullable Segment kltyton_bedrock_runtime$getSweep() {
        return kltyton_bedrock_runtime$sweep;
    }

    @Override
    public void kltyton_bedrock_runtime$setSweep(Segment sweep) {
        kltyton_bedrock_runtime$sweep = sweep;
    }
}
