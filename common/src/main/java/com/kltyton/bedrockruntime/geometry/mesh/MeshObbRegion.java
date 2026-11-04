package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.Objects;

import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;

import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

public record MeshObbRegion(AABB localBounds, int triangleCount, int positionCount) {
    public MeshObbRegion {
        Objects.requireNonNull(localBounds, "localBounds");
        if (triangleCount < 0) {
            throw new IllegalArgumentException("triangleCount must be non-negative");
        }
        if (positionCount < 0) {
            throw new IllegalArgumentException("positionCount must be non-negative");
        }
    }

    public ObbGeometry toObb(Matrix4f localToWorld) {
        return ObbGeometry.fromLocalBounds(localBounds, localToWorld);
    }
}
