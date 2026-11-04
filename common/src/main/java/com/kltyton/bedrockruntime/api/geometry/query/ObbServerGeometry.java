package com.kltyton.bedrockruntime.api.geometry.query;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbEntityGeometrySnapshot;
import com.kltyton.bedrockruntime.api.geometry.snapshot.ObbPartGeometrySnapshot;
import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;
import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * CN: 服务端 OBB 几何查询入口。数据是可信客户端发送的最新渲染姿态，不是在服务端运行 GeckoLib 得到的。
 * EN: Server OBB geometry query entrypoint. Data is the latest trusted client-rendered pose, not a server-side GeckoLib evaluation.
 */
public final class ObbServerGeometry {
    private ObbServerGeometry() {
    }

    public static Optional<ObbEntityGeometrySnapshot> snapshot(Entity entity) {
        return ServerObbStore.snapshot(entity);
    }

    public static List<com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState> animations(Entity entity) {
        return ServerObbStore.animations(entity);
    }

    public static List<ObbPartGeometrySnapshot> parts(Entity entity) {
        return snapshot(entity).map(ObbEntityGeometrySnapshot::parts).orElseGet(List::of);
    }

    public static Optional<ObbPartGeometrySnapshot> part(Entity entity, ObbBoneKey key) {
        return snapshot(entity).flatMap(value -> value.part(key));
    }

    public static List<ObbPartGeometrySnapshot> boneParts(Entity entity, String source, String boneName) {
        return snapshot(entity)
                .map(value -> value.boneParts(source, boneName))
                .orElseGet(List::of);
    }

    /**
     * CN: 在服务端当前快照上执行两个实体全部 OBB 的精确窄相位测试。
     * EN: Runs exact narrow-phase tests for every OBB pair in the two entities' current server snapshots.
     */
    public static List<PartOverlap> overlaps(Entity first, Entity second) {
        List<ObbPartGeometrySnapshot> firstParts = parts(first);
        List<ObbPartGeometrySnapshot> secondParts = parts(second);
        if (firstParts.isEmpty() || secondParts.isEmpty()) {
            return List.of();
        }

        java.util.ArrayList<PartOverlap> overlaps = new java.util.ArrayList<>();
        for (ObbPartGeometrySnapshot firstPart : firstParts) {
            for (ObbPartGeometrySnapshot secondPart : secondParts) {
                if (!firstPart.geometry().worldBounds().intersects(secondPart.geometry().worldBounds())) {
                    continue;
                }
                Optional<ObbGeometry.Overlap> overlap = firstPart.geometry().overlap(secondPart.geometry());
                overlap.ifPresent(value -> overlaps.add(new PartOverlap(firstPart, secondPart, value)));
            }
        }
        return List.copyOf(overlaps);
    }

    /** CN: 返回射线最先命中的 OBB 部位。EN: Returns the first OBB part hit by the ray segment. */
    public static Optional<RayHit> raycast(Entity entity, Vec3 from, Vec3 to) {
        return parts(entity).stream()
                .map(part -> new RayHit(part, part.geometry().clipDistanceSqr(from, to)))
                .filter(hit -> Double.isFinite(hit.distanceSqr()))
                .min(Comparator.comparingDouble(RayHit::distanceSqr));
    }

    public record RayHit(ObbPartGeometrySnapshot part, double distanceSqr) {
    }

    public record PartOverlap(
            ObbPartGeometrySnapshot firstPart,
            ObbPartGeometrySnapshot secondPart,
            ObbGeometry.Overlap overlap) {
    }
}
