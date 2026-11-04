package com.kltyton.bedrockruntime.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.kltyton.bedrockruntime.api.context.attack.ObbAttackContext;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.context.interaction.ObbInteractionContext;

import net.minecraft.world.InteractionResult;

/**
 * CN: loader 无关的 OBB 部位事件注册表。
 * EN: Loader-neutral callback registry for OBB part events.
 */
public final class BoneHitboxEvents {
    private static final List<PlayerPartAttack> PLAYER_PART_ATTACK = new CopyOnWriteArrayList<>();
    private static final List<PlayerPartHurt> PLAYER_PART_HURT = new CopyOnWriteArrayList<>();
    private static final List<PlayerPartInteract> PLAYER_PART_INTERACT = new CopyOnWriteArrayList<>();
    private static final List<PlayerPartCollision> PLAYER_PART_COLLISION = new CopyOnWriteArrayList<>();
    private static final List<EntityAttackBoxAttack> ENTITY_ATTACK_BOX_ATTACK = new CopyOnWriteArrayList<>();
    private static final List<EntityHurtBoxHurt> ENTITY_HURT_BOX_HURT = new CopyOnWriteArrayList<>();
    private static final List<EntityPartInteract> ENTITY_PART_INTERACT = new CopyOnWriteArrayList<>();
    private static final List<EntityPartCollision> ENTITY_PART_COLLISION = new CopyOnWriteArrayList<>();
    private static final List<PlayerObbCollisionAttack> PLAYER_OBB_COLLISION_ATTACK = new CopyOnWriteArrayList<>();
    private static final List<PlayerObbCollisionHurt> PLAYER_OBB_COLLISION_HURT = new CopyOnWriteArrayList<>();
    private static final List<EntityObbCollisionAttack> ENTITY_OBB_COLLISION_ATTACK = new CopyOnWriteArrayList<>();
    private static final List<EntityObbCollisionHurt> ENTITY_OBB_COLLISION_HURT = new CopyOnWriteArrayList<>();

    private BoneHitboxEvents() {
    }

    public static void registerPlayerPartAttack(PlayerPartAttack listener) {
        PLAYER_PART_ATTACK.add(listener);
    }

    public static void registerPlayerPartHurt(PlayerPartHurt listener) {
        PLAYER_PART_HURT.add(listener);
    }

    public static void registerPlayerPartInteract(PlayerPartInteract listener) {
        PLAYER_PART_INTERACT.add(listener);
    }

    public static void registerPlayerPartCollision(PlayerPartCollision listener) {
        PLAYER_PART_COLLISION.add(listener);
    }

    public static void registerEntityAttackBoxAttack(EntityAttackBoxAttack listener) {
        ENTITY_ATTACK_BOX_ATTACK.add(listener);
    }

    public static void registerEntityHurtBoxHurt(EntityHurtBoxHurt listener) {
        ENTITY_HURT_BOX_HURT.add(listener);
    }

    public static void registerEntityPartInteract(EntityPartInteract listener) {
        ENTITY_PART_INTERACT.add(listener);
    }

    public static void registerEntityPartCollision(EntityPartCollision listener) {
        ENTITY_PART_COLLISION.add(listener);
    }

    public static void registerPlayerObbCollisionAttack(PlayerObbCollisionAttack listener) {
        PLAYER_OBB_COLLISION_ATTACK.add(listener);
    }

    public static void registerPlayerObbCollisionHurt(PlayerObbCollisionHurt listener) {
        PLAYER_OBB_COLLISION_HURT.add(listener);
    }

    public static void registerEntityObbCollisionAttack(EntityObbCollisionAttack listener) {
        ENTITY_OBB_COLLISION_ATTACK.add(listener);
    }

    public static void registerEntityObbCollisionHurt(EntityObbCollisionHurt listener) {
        ENTITY_OBB_COLLISION_HURT.add(listener);
    }

    public static void firePlayerPartAttack(ObbAttackContext context) {
        PLAYER_PART_ATTACK.forEach(listener -> listener.onPlayerPartAttack(context));
    }

    public static void firePlayerPartHurt(ObbAttackContext context) {
        PLAYER_PART_HURT.forEach(listener -> listener.onPlayerPartHurt(context));
    }

    public static InteractionResult firePlayerPartInteract(ObbInteractionContext context) {
        InteractionResult result = InteractionResult.PASS;
        for (PlayerPartInteract listener : PLAYER_PART_INTERACT) {
            InteractionResult next = listener.onPlayerPartInteract(context);
            if (next != InteractionResult.PASS) {
                result = next;
            }
        }
        return result;
    }

    public static void firePlayerPartCollision(ObbCollisionContext context) {
        PLAYER_PART_COLLISION.forEach(listener -> listener.onPlayerPartCollision(context));
    }

    public static void fireEntityAttackBoxAttack(ObbAttackContext context) {
        ENTITY_ATTACK_BOX_ATTACK.forEach(listener -> listener.onEntityAttackBoxAttack(context));
    }

    public static void fireEntityHurtBoxHurt(ObbAttackContext context) {
        ENTITY_HURT_BOX_HURT.forEach(listener -> listener.onEntityHurtBoxHurt(context));
    }

    public static InteractionResult fireEntityPartInteract(ObbInteractionContext context) {
        InteractionResult result = InteractionResult.PASS;
        for (EntityPartInteract listener : ENTITY_PART_INTERACT) {
            InteractionResult next = listener.onEntityPartInteract(context);
            if (next != InteractionResult.PASS) {
                result = next;
            }
        }
        return result;
    }

    public static void fireEntityPartCollision(ObbCollisionContext context) {
        ENTITY_PART_COLLISION.forEach(listener -> listener.onEntityPartCollision(context));
    }

    public static void firePlayerObbCollisionAttack(ObbAttackContext context) {
        PLAYER_OBB_COLLISION_ATTACK.forEach(listener -> listener.onPlayerObbCollisionAttack(context));
    }

    public static void firePlayerObbCollisionHurt(ObbAttackContext context) {
        PLAYER_OBB_COLLISION_HURT.forEach(listener -> listener.onPlayerObbCollisionHurt(context));
    }

    public static void fireEntityObbCollisionAttack(ObbAttackContext context) {
        ENTITY_OBB_COLLISION_ATTACK.forEach(listener -> listener.onEntityObbCollisionAttack(context));
    }

    public static void fireEntityObbCollisionHurt(ObbAttackContext context) {
        ENTITY_OBB_COLLISION_HURT.forEach(listener -> listener.onEntityObbCollisionHurt(context));
    }

    @FunctionalInterface
    public interface PlayerPartAttack {
        void onPlayerPartAttack(ObbAttackContext context);
    }

    @FunctionalInterface
    public interface PlayerPartHurt {
        void onPlayerPartHurt(ObbAttackContext context);
    }

    @FunctionalInterface
    public interface PlayerPartInteract {
        InteractionResult onPlayerPartInteract(ObbInteractionContext context);
    }

    @FunctionalInterface
    public interface PlayerPartCollision {
        void onPlayerPartCollision(ObbCollisionContext context);
    }

    @FunctionalInterface
    public interface EntityAttackBoxAttack {
        void onEntityAttackBoxAttack(ObbAttackContext context);
    }

    @FunctionalInterface
    public interface EntityHurtBoxHurt {
        void onEntityHurtBoxHurt(ObbAttackContext context);
    }

    @FunctionalInterface
    public interface EntityPartInteract {
        InteractionResult onEntityPartInteract(ObbInteractionContext context);
    }

    @FunctionalInterface
    public interface EntityPartCollision {
        void onEntityPartCollision(ObbCollisionContext context);
    }

    /** CN: 玩家作为攻击方的纯 OBB 接触攻击事件。EN: OBB-only contact attack with a player as attacker. */
    @FunctionalInterface
    public interface PlayerObbCollisionAttack {
        void onPlayerObbCollisionAttack(ObbAttackContext context);
    }

    /** CN: 玩家作为受击方的纯 OBB 接触受击事件。EN: OBB-only contact hurt with a player as target. */
    @FunctionalInterface
    public interface PlayerObbCollisionHurt {
        void onPlayerObbCollisionHurt(ObbAttackContext context);
    }

    /** CN: 任意实体攻击 OBB 接触事件。EN: OBB-only contact attack event for any entity. */
    @FunctionalInterface
    public interface EntityObbCollisionAttack {
        void onEntityObbCollisionAttack(ObbAttackContext context);
    }

    /** CN: 任意实体受击 OBB 接触事件。EN: OBB-only contact hurt event for any entity. */
    @FunctionalInterface
    public interface EntityObbCollisionHurt {
        void onEntityObbCollisionHurt(ObbAttackContext context);
    }
}
