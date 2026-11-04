package com.kltyton.bedrockruntime.api.context.collision;

import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 一次规范化、去重后的 OBB 骨骼接触上下文。
 * EN: Context for one canonicalized and deduplicated OBB-bone contact.
 */
public record ObbCollisionContext(
        Entity firstEntity,
        ObbBoneState firstBone,
        Entity secondEntity,
        ObbBoneState secondBone,
        ObbCollisionMode collisionMode,
        ObbCollisionPhase phase,
        Vec3 correction,
        long observedGameTime,
        GeoObbAnimationState firstAnimationState,
        GeoObbAnimationState secondAnimationState) {

    public ObbCollisionContext {
        collisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
        phase = phase == null ? ObbCollisionPhase.BEGIN : phase;
        correction = correction == null ? Vec3.ZERO : correction;
        firstAnimationState = firstAnimationState == null ? GeoObbAnimationState.NONE : firstAnimationState;
        secondAnimationState = secondAnimationState == null ? GeoObbAnimationState.NONE : secondAnimationState;
    }

    public ObbCollisionContext(Entity firstEntity, ObbBoneState firstBone, Entity secondEntity, ObbBoneState secondBone,
            ObbCollisionMode collisionMode, ObbCollisionPhase phase, Vec3 correction,
            GeoObbAnimationState firstAnimationState, GeoObbAnimationState secondAnimationState) {
        this(firstEntity, firstBone, secondEntity, secondBone, collisionMode, phase, correction, -1L,
                firstAnimationState, secondAnimationState);
    }

    public boolean hardCollision() {
        return collisionMode == ObbCollisionMode.HARD;
    }

    /** CN: 将第一个 OBB 推离第二个 OBB 的单位法线。EN: Unit normal moving the first OBB away from the second. */
    public Vec3 normal() {
        return correction.lengthSqr() <= 1.0E-12 ? Vec3.ZERO : correction.normalize();
    }

    public double penetrationDepth() {
        return correction.length();
    }

    /** CN: 第一个实体相对第二个实体的速度。EN: Velocity of the first entity relative to the second. */
    public Vec3 relativeVelocity() {
        return firstEntity.getDeltaMovement().subtract(secondEntity.getDeltaMovement());
    }

    public ObbCollisionContext reversed() {
        return new ObbCollisionContext(
                secondEntity,
                secondBone,
                firstEntity,
                firstBone,
                collisionMode,
                phase,
                correction.scale(-1.0),
                observedGameTime,
                secondAnimationState,
                firstAnimationState);
    }
}
