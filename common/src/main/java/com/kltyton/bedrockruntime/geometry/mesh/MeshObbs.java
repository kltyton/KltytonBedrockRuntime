package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.List;
import java.util.Objects;

import net.minecraft.world.phys.AABB;

public final class MeshObbs {
    private final List<MeshObbRegion> regions;
    private final AABB bounds;

    public MeshObbs(List<MeshObbRegion> regions) {
        Objects.requireNonNull(regions, "regions");
        this.regions = List.copyOf(regions);
        if (this.regions.isEmpty()) {
            this.bounds = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        } else {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (MeshObbRegion region : this.regions) {
                AABB local = region.localBounds();
                minX = Math.min(minX, local.minX);
                minY = Math.min(minY, local.minY);
                minZ = Math.min(minZ, local.minZ);
                maxX = Math.max(maxX, local.maxX);
                maxY = Math.max(maxY, local.maxY);
                maxZ = Math.max(maxZ, local.maxZ);
            }
            this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static MeshObbs empty() {
        return new MeshObbs(List.of());
    }

    public List<MeshObbRegion> regions() {
        return regions;
    }

    public AABB bounds() {
        return bounds;
    }

    public int regionCount() {
        return regions.size();
    }

    public int triangleCount() {
        int total = 0;
        for (MeshObbRegion region : regions) {
            total += region.triangleCount();
        }
        return total;
    }
}
