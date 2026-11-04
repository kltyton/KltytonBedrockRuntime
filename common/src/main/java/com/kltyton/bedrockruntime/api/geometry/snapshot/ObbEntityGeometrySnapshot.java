package com.kltyton.bedrockruntime.api.geometry.snapshot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 一个实体最近两帧客户端模型 OBB 的服务端只读快照。
 * EN: Server-side read-only snapshot of an entity's two latest client-model OBB frames.
 */
public record ObbEntityGeometrySnapshot(
        Entity entity,
        long snapshotGameTime,
        long previousSnapshotGameTime,
        GeoObbAnimationState animationState,
        List<ObbPartGeometrySnapshot> parts,
        List<ObbPartGeometrySnapshot> previousParts,
        Vec3 rootPosition,
        Vec3 previousRootPosition,
        float rootYaw,
        float previousRootYaw) {

    public ObbEntityGeometrySnapshot {
        Objects.requireNonNull(entity, "entity");
        animationState = animationState == null ? GeoObbAnimationState.NONE : animationState;
        parts = List.copyOf(parts);
        previousParts = List.copyOf(previousParts);
        rootPosition = rootPosition == null ? entity.position() : rootPosition;
        previousRootPosition = previousRootPosition == null ? rootPosition : previousRootPosition;
    }

    public Optional<ObbPartGeometrySnapshot> part(ObbBoneKey key) {
        return parts.stream().filter(part -> part.key().equals(key)).findFirst();
    }

    public Optional<ObbPartGeometrySnapshot> previousPart(ObbBoneKey key) {
        return previousParts.stream().filter(part -> part.key().equals(key)).findFirst();
    }

    /** CN: 返回同一来源、同一骨骼组下的全部 cube。EN: Returns every cube under one source and bone group. */
    public List<ObbPartGeometrySnapshot> boneParts(String source, String boneName) {
        return parts.stream()
                .filter(part -> part.key().source().equals(source) && part.key().name().equals(boneName))
                .toList();
    }

    /** CN: 当前服务端 tick 下该渲染快照的年龄。EN: Age of this rendered snapshot at the current server tick. */
    public long ageTicks() {
        return Math.max(0L, entity.level().getGameTime() - snapshotGameTime);
    }
}
