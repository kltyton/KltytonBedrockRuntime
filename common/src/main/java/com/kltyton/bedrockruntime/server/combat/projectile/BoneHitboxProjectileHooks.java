package com.kltyton.bedrockruntime.server.combat.projectile;

import com.kltyton.bedrockruntime.api.bone.builtin.ObbBuiltinBones;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.geometry.query.ObbServerGeometry;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.server.combat.attack.BoneHitboxAttackBoxHooks;
import com.kltyton.bedrockruntime.server.combat.damage.VanillaDamageCapture;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore.AttackContact;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/** Resolves the struck part before vanilla moves or damages its target, then dispatches the damage result. */
public final class BoneHitboxProjectileHooks {
    private BoneHitboxProjectileHooks() {
    }

    public static @Nullable AttackContact resolveArrowHit(Projectile projectile, EntityHitResult hit) {
        if (projectile.level().isClientSide()
                || !(projectile instanceof BoneHitboxEntity attacker)
                || !(hit.getEntity() instanceof BoneHitboxEntity)) {
            return null;
        }
        ProjectileHitSweep.Segment sweep = ((ProjectileHitSweep) hit).kltyton_bedrock_runtime$getSweep();
        if (sweep == null) {
            return null;
        }
        return attacker.kltyton_bedrock_runtime$obbState().resolve(ObbBuiltinBones.projectile(projectile))
                .filter(ObbBoneState::isAttackBox)
                .flatMap(attackBone -> ObbServerGeometry.raycast(hit.getEntity(), sweep.from(), sweep.to(),
                        part -> part.bone().isHurtBox()).map(result -> new AttackContact(attackBone, result.part().bone())))
                .orElse(null);
    }

    public static void afterHit(Projectile projectile, EntityHitResult hit, @Nullable AttackContact contact) {
        if (projectile.level().isClientSide() || !(hit.getEntity() instanceof BoneHitboxEntity)) {
            return;
        }
        Entity target = hit.getEntity();
        if (((ProjectileHitSweep) hit).kltyton_bedrock_runtime$getSweep() == null) {
            contact = ServerObbContactStore.findAttack(projectile, target).orElse(null);
        }
        if (contact != null) {
            BoneHitboxAttackBoxHooks.handleVanillaAttack(projectile, target, contact.attackBone(), contact.hurtBone(),
                    VanillaDamageCapture.consume(projectile, target));
        }
    }
}
