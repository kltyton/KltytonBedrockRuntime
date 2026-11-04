package com.kltyton.bedrockruntime.geometry.mesh;

public record MeshFittingOptions(int maxRegions, int maxTrianglesPerRegion, double minRegionExtent, double padding) {
    public MeshFittingOptions {
        if (maxRegions < 1) {
            throw new IllegalArgumentException("maxRegions must be at least one");
        }
        if (maxTrianglesPerRegion < 1) {
            throw new IllegalArgumentException("maxTrianglesPerRegion must be at least one");
        }
        if (!Double.isFinite(minRegionExtent) || minRegionExtent < 0.0) {
            throw new IllegalArgumentException("minRegionExtent must be finite and non-negative");
        }
        if (!Double.isFinite(padding) || padding < 0.0) {
            throw new IllegalArgumentException("padding must be finite and non-negative");
        }
    }

    public static MeshFittingOptions defaults() {
        return new MeshFittingOptions(64, 64, 1.0 / 64.0, 0.0);
    }
}
