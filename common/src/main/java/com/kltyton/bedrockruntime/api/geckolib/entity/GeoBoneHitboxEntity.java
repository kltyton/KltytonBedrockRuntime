package com.kltyton.bedrockruntime.api.geckolib.entity;

import com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxState;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;

/**
 * CN: GeckoLib/Geo 渲染实体专用 OBB 扩展接口。
 * EN: OBB extension interface for GeckoLib/Geo-rendered entities.
 */
public interface GeoBoneHitboxEntity extends BoneHitboxEntity {
    String KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE = "gecko";
    String KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE = "gecko_animation";

    /** CN: 读取永久骨骼属性组。EN: Returns a permanent bone-attribute group. */
    default Set<String> kltyton_bedrock_runtime$geoPermanentGroups(ObbBoneAttribute attribute) {
        return kltyton_bedrock_runtime$obbState().persistentAttributeBones(KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE, attribute);
    }

    /** CN: 覆盖一个永久骨骼属性组。EN: Replaces one permanent bone-attribute group. */
    default void kltyton_bedrock_runtime$geoSetPermanentGroups(ObbBoneAttribute attribute, Collection<String> groupNames) {
        kltyton_bedrock_runtime$obbState().setPersistentAttributeBones(KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE, attribute, groupNames);
    }

    /** CN: 向一个永久骨骼属性组追加名称。EN: Adds names to one permanent bone-attribute group. */
    default void kltyton_bedrock_runtime$geoAddPermanentGroups(ObbBoneAttribute attribute, Collection<String> groupNames) {
        Set<String> merged = new LinkedHashSet<>(kltyton_bedrock_runtime$geoPermanentGroups(attribute));
        if (groupNames != null) {
            merged.addAll(groupNames);
        }
        kltyton_bedrock_runtime$geoSetPermanentGroups(attribute, merged);
    }

    default void kltyton_bedrock_runtime$geoClearPermanentGroups(ObbBoneAttribute attribute) {
        kltyton_bedrock_runtime$geoSetPermanentGroups(attribute, Set.of());
    }

    /**
     * CN: 在当前动画作用域中临时设置骨骼属性；动画结束或切换后自动恢复。
     * EN: Temporarily sets a bone attribute in the current animation scope and restores it after the animation ends or changes.
     */
    default void kltyton_bedrock_runtime$geoSetTemporaryGroups(ObbBoneAttribute attribute, Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(kltyton_bedrock_runtime$geoAnimationState(), attribute, groupNames);
    }

    default void kltyton_bedrock_runtime$geoSetTemporaryGroups(GeoObbAnimationState animationState,
            ObbBoneAttribute attribute, Collection<String> groupNames) {
        kltyton_bedrock_runtime$obbState().setTemporaryAttributeBones(
                KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE,
                KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE,
                attribute,
                groupNames);
        GeoBoneHitboxState.bindTemporaryWindow(this, animationState);
    }

    default Set<String> kltyton_bedrock_runtime$geoTemporaryGroups(ObbBoneAttribute attribute) {
        return kltyton_bedrock_runtime$obbState().temporaryAttributeBones(
                KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE,
                KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE,
                attribute);
    }

    /** CN: 临时设置软/硬碰撞骨骼组。EN: Temporarily sets a soft/hard collision-bone group. */
    default void kltyton_bedrock_runtime$geoSetTemporaryCollisionGroups(ObbCollisionMode mode, Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetTemporaryCollisionGroups(kltyton_bedrock_runtime$geoAnimationState(), mode, groupNames);
    }

    default void kltyton_bedrock_runtime$geoSetTemporaryCollisionGroups(GeoObbAnimationState animationState,
            ObbCollisionMode mode, Collection<String> groupNames) {
        kltyton_bedrock_runtime$obbState().setTemporaryCollisionBones(
                KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE,
                KLTYTON_BEDROCK_RUNTIME_GEO_SOURCE,
                mode,
                groupNames);
        GeoBoneHitboxState.bindTemporaryWindow(this, animationState);
    }

    /** CN: 清除本次动画的全部临时属性。EN: Clears all temporary attributes for the current animation. */
    default void kltyton_bedrock_runtime$geoClearTemporaryGroups() {
        kltyton_bedrock_runtime$obbState().clearTemporaryAttributeScope(KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE);
        GeoBoneHitboxState.clearTemporaryWindow(this);
    }

    default Set<String> kltyton_bedrock_runtime$geoPermanentAttackGroups() {
        return kltyton_bedrock_runtime$geoPermanentGroups(ObbBoneAttribute.ATTACK);
    }

    default void kltyton_bedrock_runtime$geoSetPermanentAttackGroups(Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetPermanentGroups(ObbBoneAttribute.ATTACK, groupNames);
    }

    default void kltyton_bedrock_runtime$geoAddPermanentAttackGroups(Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoAddPermanentGroups(ObbBoneAttribute.ATTACK, groupNames);
    }

    default void kltyton_bedrock_runtime$geoClearPermanentAttackGroups() {
        kltyton_bedrock_runtime$geoClearPermanentGroups(ObbBoneAttribute.ATTACK);
    }

    default Set<String> kltyton_bedrock_runtime$geoTemporaryAttackGroups() {
        return kltyton_bedrock_runtime$geoTemporaryGroups(ObbBoneAttribute.ATTACK);
    }

    default void kltyton_bedrock_runtime$geoSetTemporaryAttackGroups(Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(ObbBoneAttribute.ATTACK, groupNames);
    }

    default void kltyton_bedrock_runtime$geoSetTemporaryAttackGroups(GeoObbAnimationState animationState,
            Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(animationState, ObbBoneAttribute.ATTACK, groupNames);
    }

    default void kltyton_bedrock_runtime$geoClearTemporaryAttackGroups() {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(ObbBoneAttribute.ATTACK, Set.of());
    }

    default void kltyton_bedrock_runtime$geoSetTemporaryHurtGroups(Collection<String> groupNames) {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(ObbBoneAttribute.HURT, groupNames);
    }

    default Set<String> kltyton_bedrock_runtime$geoTemporaryHurtGroups() {
        return kltyton_bedrock_runtime$geoTemporaryGroups(ObbBoneAttribute.HURT);
    }

    default void kltyton_bedrock_runtime$geoClearTemporaryHurtGroups() {
        kltyton_bedrock_runtime$geoSetTemporaryGroups(ObbBoneAttribute.HURT, Set.of());
    }

    /** CN: 当前服务端/客户端已知的 Geo 动画状态。EN: Current Geo animation state known to this side. */
    default GeoObbAnimationState kltyton_bedrock_runtime$geoAnimationState() {
        return GeoBoneHitboxState.animationState(this);
    }

    /**
     * CN: 由库在收到模型状态或渲染状态时调用，用户通常不需要手动调用。
     * EN: Called by the library when model/render state is received; users normally do not call this manually.
     */
    default void kltyton_bedrock_runtime$geoUpdateAnimationStates(java.util.List<GeoObbAnimationState> states) {
        if (GeoBoneHitboxState.updateAnimationStates(this, states)) {
            kltyton_bedrock_runtime$obbState().clearTemporaryAttributeScope(KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE);
        }
    }

    default void kltyton_bedrock_runtime$geoUpdateAnimationState(GeoObbAnimationState animationState) {
        if (GeoBoneHitboxState.updateAnimationState(this, animationState)) {
            kltyton_bedrock_runtime$obbState().clearTemporaryAttributeScope(KLTYTON_BEDROCK_RUNTIME_ANIMATION_SCOPE);
        }
    }
}
