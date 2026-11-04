package com.kltyton.bedrockruntime.api.entity;

import java.util.Optional;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.context.attack.ObbAttackContext;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.context.interaction.ObbInteractionContext;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;
import com.kltyton.bedrockruntime.api.state.runtime.ObbEntityState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbEntityStates;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;

import net.minecraft.world.InteractionResult;

/**
 * CN: 实体显式 opt-in 接口。实现者必须注册哪些视觉模型骨骼/cube 应成为 OBB。
 * EN: Explicit entity opt-in interface. Implementors must register which visual-model bones/cubes become OBBs.
 */
public interface BoneHitboxEntity {
    /**
     * CN: 注册该实体的 OBB 骨骼。可使用 ObbBoneRegistrar.ALL、BASE 或精确名称。
     * EN: Registers this entity's OBB bones using ObbBoneRegistrar.ALL, BASE, or exact names.
     */
    void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar);

    /** Restricts tracked clients allowed to supply model poses; override for an authoritative owner. */
    default boolean kltyton_bedrock_runtime$acceptObbReporter(net.minecraft.server.level.ServerPlayer reporter) {
        return true;
    }

    /** Maximum model distance from the entity root, in blocks. Large models may explicitly raise this limit. */
    default double kltyton_bedrock_runtime$maxSnapshotRadius() {
        return 64.0;
    }

    /** CN: 返回该实体的持久/运行时 OBB 状态。EN: Returns this entity's persistent/runtime OBB state. */
    default ObbEntityState kltyton_bedrock_runtime$obbState() {
        return ObbEntityStates.get(this);
    }

    /** CN: 获取一个已注册/已发现的 OBB 骨骼。EN: Resolves one registered and discovered OBB bone. */
    default Optional<ObbBoneState> kltyton_bedrock_runtime$obbBone(ObbBoneKey key) {
        return kltyton_bedrock_runtime$obbState().resolve(key);
    }

    default boolean kltyton_bedrock_runtime$hasObbAttribute(ObbBoneKey key, ObbBoneAttribute attribute) {
        return kltyton_bedrock_runtime$obbBone(key).map(bone -> bone.hasAttribute(attribute)).orElse(false);
    }

    /** CN: 永久开关一个骨骼属性，包括注册时设置的属性。EN: Permanently toggles a bone attribute, including a registered default. */
    default boolean kltyton_bedrock_runtime$setObbAttribute(ObbBoneKey key, ObbBoneAttribute attribute, boolean enabled) {
        Optional<ObbBoneState> bone = kltyton_bedrock_runtime$obbBone(key);
        bone.ifPresent(value -> value.setAttribute(attribute, enabled));
        return bone.isPresent();
    }

    /** CN: 在作用域中临时开关属性，清除作用域后自动恢复。EN: Temporarily toggles an attribute until its scope is cleared. */
    default boolean kltyton_bedrock_runtime$setTemporaryObbAttribute(String scope, ObbBoneKey key,
            ObbBoneAttribute attribute, boolean enabled) {
        Optional<ObbBoneState> bone = kltyton_bedrock_runtime$obbBone(key);
        bone.ifPresent(value -> value.setTemporaryAttribute(scope, attribute, enabled));
        return bone.isPresent();
    }

    default void kltyton_bedrock_runtime$clearTemporaryObbAttributes(String scope) {
        kltyton_bedrock_runtime$obbState().clearTemporaryAttributeScope(scope);
    }

    default boolean kltyton_bedrock_runtime$isAttackBox(ObbBoneKey key) {
        return kltyton_bedrock_runtime$obbBone(key).map(ObbBoneState::isAttackBox).orElse(false);
    }

    default boolean kltyton_bedrock_runtime$setAttackBox(ObbBoneKey key, boolean enabled) {
        return kltyton_bedrock_runtime$setObbAttribute(key, ObbBoneAttribute.ATTACK, enabled);
    }

    default boolean kltyton_bedrock_runtime$isHurtBox(ObbBoneKey key) {
        return kltyton_bedrock_runtime$obbBone(key).map(ObbBoneState::isHurtBox).orElse(false);
    }

    default boolean kltyton_bedrock_runtime$setHurtBox(ObbBoneKey key, boolean enabled) {
        return kltyton_bedrock_runtime$setObbAttribute(key, ObbBoneAttribute.HURT, enabled);
    }

    default boolean kltyton_bedrock_runtime$isCollisionBox(ObbBoneKey key) {
        return kltyton_bedrock_runtime$obbBone(key).map(ObbBoneState::isCollisionBox).orElse(false);
    }

    default boolean kltyton_bedrock_runtime$setCollisionBox(ObbBoneKey key, ObbCollisionMode mode) {
        Optional<ObbBoneState> bone = kltyton_bedrock_runtime$obbBone(key);
        bone.ifPresent(value -> value.setCollisionMode(mode));
        return bone.isPresent();
    }

    default boolean kltyton_bedrock_runtime$setTemporaryAttackBox(String scope, ObbBoneKey key, boolean enabled) {
        return kltyton_bedrock_runtime$setTemporaryObbAttribute(scope, key, ObbBoneAttribute.ATTACK, enabled);
    }

    default boolean kltyton_bedrock_runtime$setTemporaryHurtBox(String scope, ObbBoneKey key, boolean enabled) {
        return kltyton_bedrock_runtime$setTemporaryObbAttribute(scope, key, ObbBoneAttribute.HURT, enabled);
    }

    default boolean kltyton_bedrock_runtime$setTemporaryCollisionBox(String scope, ObbBoneKey key, ObbCollisionMode mode) {
        Optional<ObbBoneState> bone = kltyton_bedrock_runtime$obbBone(key);
        bone.ifPresent(value -> value.setTemporaryCollision(scope, mode));
        return bone.isPresent();
    }

    /**
     * CN: 是否用命中部位闪红替代全身受击闪红。
     * EN: Whether the full-body hurt flash should be replaced by the hit-part flash.
     */
    default boolean kltyton_bedrock_runtime$onlyHitPartTurnsRed() {
        return true;
    }

    /**
     * CN: 原版攻击已发生、攻击骨骼回调派发前调用；返回 false 会停止 OBB 扩展回调，但不会撤销原版攻击。
     * EN: Called before attack-bone callbacks after the vanilla attack occurs; false stops OBB extension callbacks but does not undo vanilla damage.
     */
    default boolean kltyton_bedrock_runtime$beforeAttackBoxAttack(ObbAttackContext context) {
        return true;
    }

    /** CN: 攻击骨骼主回调。EN: Main attack-bone callback. */
    default void kltyton_bedrock_runtime$onAttackBoxAttack(ObbAttackContext context) {
    }

    /** CN: 攻击骨骼回调链结束后调用。EN: Called after the attack-bone callback chain. */
    default void kltyton_bedrock_runtime$afterAttackBoxAttack(ObbAttackContext context) {
    }

    /**
     * CN: 受击骨骼主回调前调用；返回 false 只停止 OBB 受击扩展回调。
     * EN: Called before hurt-bone callbacks; false only stops OBB hurt extension callbacks.
     */
    default boolean kltyton_bedrock_runtime$beforeHurtBoxHurt(ObbAttackContext context) {
        return true;
    }

    /** CN: 受击骨骼主回调。EN: Main hurt-bone callback. */
    default void kltyton_bedrock_runtime$onHurtBoxHurt(ObbAttackContext context) {
    }

    /** CN: 受击骨骼回调链结束后调用。EN: Called after the hurt-bone callback chain. */
    default void kltyton_bedrock_runtime$afterHurtBoxHurt(ObbAttackContext context) {
    }

    /**
     * CN: 骨骼交互前调用；非 PASS 结果会跳过主交互回调并直接作为 OBB 交互结果。
     * EN: Called before bone interaction; a non-PASS result skips the main interaction callback and becomes the OBB interaction result.
     */
    default InteractionResult kltyton_bedrock_runtime$beforeObbInteract(ObbInteractionContext context) {
        return InteractionResult.PASS;
    }

    /** CN: 骨骼交互主回调。EN: Main bone-interaction callback. */
    default InteractionResult kltyton_bedrock_runtime$onObbInteract(ObbInteractionContext context) {
        return InteractionResult.PASS;
    }

    /** CN: 骨骼交互结果确定后调用。EN: Called after the bone-interaction result is determined. */
    default void kltyton_bedrock_runtime$afterObbInteract(ObbInteractionContext context, InteractionResult result) {
    }

    /**
     * CN: 规范化碰撞的 BEGIN 或 END 阶段回调；同一连续骨骼对接触不会因多客户端上报而重复 BEGIN。
     * EN: Called for BEGIN or END of a canonical contact; one continuous bone-pair contact does not duplicate BEGIN across reporters.
     */
    default void kltyton_bedrock_runtime$onObbCollision(ObbCollisionContext context) {
    }

    /**
     * CN: 纯 OBB 接触攻击事件前置 hook；它不代表原版攻击或伤害已经发生。
     * EN: Pre-hook for an OBB-only contact attack; it does not imply vanilla attack or damage occurred.
     */
    default boolean kltyton_bedrock_runtime$beforeObbCollisionAttack(ObbAttackContext context) {
        return true;
    }

    default void kltyton_bedrock_runtime$onObbCollisionAttack(ObbAttackContext context) {
    }

    default void kltyton_bedrock_runtime$afterObbCollisionAttack(ObbAttackContext context) {
    }

    /** CN: 纯 OBB 接触受击事件前置 hook。EN: Pre-hook for an OBB-only contact hurt event. */
    default boolean kltyton_bedrock_runtime$beforeObbCollisionHurt(ObbAttackContext context) {
        return true;
    }

    default void kltyton_bedrock_runtime$onObbCollisionHurt(ObbAttackContext context) {
    }

    default void kltyton_bedrock_runtime$afterObbCollisionHurt(ObbAttackContext context) {
    }
}
