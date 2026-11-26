package com.kltyton.bedrockruntime.server.combat.projectile;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Carries the vanilla arrow collision ray on its own hit result until damage is applied. */
public interface ProjectileHitSweep {
    static void capture(EntityHitResult result, Vec3 from, Vec3 to) {
        ((ProjectileHitSweep) result).kltyton_bedrock_runtime$setSweep(new Segment(from, to));
    }

    @Nullable Segment kltyton_bedrock_runtime$getSweep();

    void kltyton_bedrock_runtime$setSweep(Segment sweep);

    record Segment(Vec3 from, Vec3 to) {
    }
}
