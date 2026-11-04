package com.kltyton.bedrockruntime.api.geometry.snapshot;

import java.util.List;
import java.util.Objects;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;

import net.minecraft.world.phys.Vec3;

/**
 * CN: 服务端可读的单个视觉骨骼/cube OBB 快照。几何来自最近一次客户端渲染姿态同步。
 * EN: Server-readable snapshot of one visual bone/cube OBB. Geometry comes from the latest client-rendered pose sync.
 */
public record ObbPartGeometrySnapshot(ObbBoneState bone, ObbGeometry geometry) {
    public ObbPartGeometrySnapshot {
        Objects.requireNonNull(bone, "bone");
        Objects.requireNonNull(geometry, "geometry");
    }

    public ObbBoneKey key() {
        return bone.key();
    }

    /** CN: 该部位在世界空间中的 8 个顶点。EN: Eight world-space vertices of this part. */
    public List<Vec3> worldVertices() {
        return geometry.worldVertices();
    }

    /** CN: cube 本地坐标原点在世界空间中的位置。EN: World-space position of the cube's local coordinate origin. */
    public Vec3 worldOrigin() {
        return geometry.toWorldPosition(Vec3.ZERO);
    }

    public Vec3 worldCenter() {
        return geometry.worldCenter();
    }

    public List<Vec3> worldAxes() {
        return geometry.worldAxes();
    }

    public Vec3 worldHalfExtents() {
        return geometry.worldHalfExtents();
    }
}
