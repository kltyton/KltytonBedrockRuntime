package com.kltyton.bedrockruntime.api.geckolib.skill.context;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbEntityGeometrySnapshot;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbPartGeometrySnapshot;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 服务端关键帧技能回调的完整上下文，包括客户端动画时钟和最近的逐骨骼 OBB 姿态。
 * EN: Full server keyframe-skill context, including the client animation clock and latest per-bone OBB pose.
 */
public record GeoKeyframeSkillContext(
        ServerPlayer reporter,
        Entity entity,
        String skillId,
        String marker,
        GeoKeyframeSkillPhase phase,
        GeoObbAnimationState animationState,
        double markerTimeSeconds,
        double animationSpeed,
        long clientSequence,
        long observedGameTime,
        long serverGameTime,
        Vec3 entityPosition,
        Vec3 entityVelocity,
        float entityYaw,
        float entityPitch,
        Optional<ObbEntityGeometrySnapshot> geometrySnapshot) {

    public GeoKeyframeSkillContext {
        Objects.requireNonNull(reporter, "reporter");
        Objects.requireNonNull(entity, "entity");
        skillId = skillId == null ? "" : skillId;
        marker = marker == null ? "" : marker;
        phase = phase == null ? GeoKeyframeSkillPhase.POINT : phase;
        animationState = animationState == null ? GeoObbAnimationState.NONE : animationState;
        entityPosition = entityPosition == null ? entity.position() : entityPosition;
        entityVelocity = entityVelocity == null ? entity.getDeltaMovement() : entityVelocity;
        geometrySnapshot = geometrySnapshot == null ? Optional.empty() : geometrySnapshot;
    }

    public ServerLevel level() {
        return (ServerLevel) entity.level();
    }

    public MinecraftServer server() {
        return level().getServer();
    }

    /** CN: Mob 当前服务端 AI 目标。EN: Current server-side AI target when the subject is a mob. */
    public Optional<LivingEntity> target() {
        return entity instanceof Mob mob ? Optional.ofNullable(mob.getTarget()) : Optional.empty();
    }

    /** CN: 技能实体为投射物时返回其拥有者。EN: Returns the owner when the skill subject is a projectile. */
    public Optional<Entity> owner() {
        return entity instanceof Projectile projectile ? Optional.ofNullable(projectile.getOwner()) : Optional.empty();
    }

    public List<ObbPartGeometrySnapshot> parts() {
        return geometrySnapshot.map(ObbEntityGeometrySnapshot::parts).orElseGet(List::of);
    }

    public Optional<ObbPartGeometrySnapshot> part(ObbBoneKey key) {
        return geometrySnapshot.flatMap(snapshot -> snapshot.part(key));
    }
}
