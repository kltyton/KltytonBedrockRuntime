package com.kltyton.bedrockruntime.api.context.attack;

import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.context.damage.ObbDamageInfo;

import java.util.Optional;

import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * CN: 原版攻击或纯 OBB 接触攻击的统一部位上下文。两类事件使用独立监听器。
 * EN: Unified part context for vanilla attacks and OBB-only contact attacks. The two paths use separate listeners.
 */
public record ObbAttackContext(
        Player player,
        Entity attacker,
        Entity target,
        ObbBoneState attackBone,
        ObbBoneState hurtBone,
        ObbAttackOrigin origin,
        ObbDamageInfo damage,
        Optional<ObbCollisionContext> collision,
        GeoObbAnimationState attackerAnimationState,
        GeoObbAnimationState targetAnimationState) {

    public ObbAttackContext {
        origin = origin == null ? ObbAttackOrigin.VANILLA : origin;
        damage = damage == null ? ObbDamageInfo.NONE : damage;
        collision = collision == null ? Optional.empty() : collision;
        attackerAnimationState = attackerAnimationState == null ? GeoObbAnimationState.NONE : attackerAnimationState;
        targetAnimationState = targetAnimationState == null ? GeoObbAnimationState.NONE : targetAnimationState;
    }

    public static ObbAttackContext vanilla(Player player, Entity attacker, Entity target, ObbBoneState attackBone,
            ObbBoneState hurtBone, ObbDamageInfo damage, GeoObbAnimationState attackerAnimationState,
            GeoObbAnimationState targetAnimationState) {
        return new ObbAttackContext(
                player,
                attacker,
                target,
                attackBone,
                hurtBone,
                ObbAttackOrigin.VANILLA,
                damage,
                Optional.empty(),
                attackerAnimationState,
                targetAnimationState);
    }

    public static ObbAttackContext obbContact(ObbCollisionContext collision) {
        return new ObbAttackContext(
                collision.firstEntity() instanceof Player player ? player : null,
                collision.firstEntity(),
                collision.secondEntity(),
                collision.firstBone(),
                collision.secondBone(),
                ObbAttackOrigin.OBB_CONTACT,
                ObbDamageInfo.NONE,
                Optional.of(collision),
                collision.firstAnimationState(),
                collision.secondAnimationState());
    }

    /** CN: 攻击方实际造成的生命值加吸收值损失。EN: Health plus absorption loss dealt by the attacker. */
    public float damageDealt() {
        return damage.appliedDamage();
    }

    /** CN: 受击方实际承受的生命值加吸收值损失。EN: Health plus absorption loss received by the target. */
    public float damageReceived() {
        return damage.appliedDamage();
    }

    public boolean vanillaAttack() {
        return origin == ObbAttackOrigin.VANILLA;
    }

    public boolean obbContactAttack() {
        return origin == ObbAttackOrigin.OBB_CONTACT;
    }
}
