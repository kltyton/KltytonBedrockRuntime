package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.world.phys.AABB;

public final class MeshObbFitter {
    private static final double WELD_SCALE = 100000.0;
    private static final double MIN_REGION_THICKNESS = 1.0E-4;

    private MeshObbFitter() {
    }

    public static MeshObbs fit(TriangleMesh mesh) {
        return fit(mesh, MeshFittingOptions.defaults());
    }

    public static MeshObbs fit(TriangleMesh mesh, MeshFittingOptions options) {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(options, "options");
        if (mesh.triangleCount() == 0) {
            return MeshObbs.empty();
        }

        int[] parents = new int[mesh.triangleCount()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = i;
        }
        Map<EdgeKey, Integer> edgeOwners = new HashMap<>();
        for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
            VertexKey first = vertexKey(mesh, triangle, 0);
            VertexKey second = vertexKey(mesh, triangle, 1);
            VertexKey third = vertexKey(mesh, triangle, 2);
            connect(parents, edgeOwners, EdgeKey.of(first, second), triangle);
            connect(parents, edgeOwners, EdgeKey.of(second, third), triangle);
            connect(parents, edgeOwners, EdgeKey.of(third, first), triangle);
        }

        Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
        for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
            int root = find(parents, triangle);
            grouped.computeIfAbsent(root, ignored -> new ArrayList<>()).add(triangle);
        }
        List<Region> components = new ArrayList<>(grouped.size());
        for (List<Integer> triangles : grouped.values()) {
            components.add(new Region(toIntArray(triangles), mesh));
        }
        components = budgetComponents(components, mesh, options.maxRegions());

        PriorityQueue<Region> pending = new PriorityQueue<>(regionComparator());
        pending.addAll(components);
        List<Region> finished = new ArrayList<>();
        while (!pending.isEmpty()) {
            Region region = pending.poll();
            boolean canSplit = finished.size() + pending.size() + 2 <= options.maxRegions()
                    && region.triangles.length > options.maxTrianglesPerRegion()
                    && region.maxExtent() > options.minRegionExtent();
            if (canSplit) {
                Region[] split = split(region, options.minRegionExtent());
                if (split != null) {
                    pending.add(split[0]);
                    pending.add(split[1]);
                    continue;
                }
            }
            finished.add(region);
        }
        finished.sort(Comparator.comparingDouble(Region::minX)
                .thenComparingDouble(Region::minY)
                .thenComparingDouble(Region::minZ));

        List<MeshObbRegion> regions = new ArrayList<>(finished.size());
        for (Region region : finished) {
            regions.add(new MeshObbRegion(region.expanded(options.padding()), region.triangles.length,
                    region.uniquePositionCount()));
        }
        return new MeshObbs(regions);
    }

    private static List<Region> budgetComponents(List<Region> components, TriangleMesh mesh, int maximum) {
        if (components.size() <= maximum) {
            return components;
        }
        List<Region> ordered = new ArrayList<>(components);
        ordered.sort(regionComparator());
        int keep = Math.max(0, maximum - 1);
        List<Region> result = new ArrayList<>(maximum);
        for (int i = 0; i < keep && i < ordered.size(); i++) {
            result.add(ordered.get(i));
        }
        int triangleCount = 0;
        for (int i = keep; i < ordered.size(); i++) {
            triangleCount += ordered.get(i).triangles.length;
        }
        int[] merged = new int[triangleCount];
        int offset = 0;
        for (int i = keep; i < ordered.size(); i++) {
            int[] triangles = ordered.get(i).triangles;
            System.arraycopy(triangles, 0, merged, offset, triangles.length);
            offset += triangles.length;
        }
        result.add(new Region(merged, mesh));
        return result;
    }

    private static Region[] split(Region region, double minimumExtent) {
        double sizeX = region.maxX - region.minX;
        double sizeY = region.maxY - region.minY;
        double sizeZ = region.maxZ - region.minZ;
        int axis = sizeX >= sizeY && sizeX >= sizeZ ? 0 : (sizeY >= sizeZ ? 1 : 2);
        if (Math.max(sizeX, Math.max(sizeY, sizeZ)) <= minimumExtent || region.triangles.length < 2) {
            return null;
        }
        Integer[] order = new Integer[region.triangles.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = region.triangles[i];
        }
        Arrays.sort(order, Comparator
                .comparingDouble((Integer triangle) -> region.centroid(axis, triangle))
                .thenComparingInt(Integer::intValue));
        int middle = order.length / 2;
        if (middle <= 0 || middle >= order.length) {
            return null;
        }
        int[] first = new int[middle];
        int[] second = new int[order.length - middle];
        for (int i = 0; i < middle; i++) {
            first[i] = order[i];
        }
        for (int i = middle; i < order.length; i++) {
            second[i - middle] = order[i];
        }
        return new Region[]{ new Region(first, region.mesh), new Region(second, region.mesh) };
    }

    private static Comparator<Region> regionComparator() {
        return Comparator.comparingInt((Region region) -> region.triangles.length).reversed()
                .thenComparing(Comparator.comparingDouble(Region::maxExtent).reversed())
                .thenComparingDouble(Region::minX)
                .thenComparingDouble(Region::minY)
                .thenComparingDouble(Region::minZ);
    }

    private static void connect(int[] parents, Map<EdgeKey, Integer> owners, EdgeKey edge, int triangle) {
        Integer owner = owners.putIfAbsent(edge, triangle);
        if (owner != null) {
            union(parents, owner, triangle);
        }
    }

    private static int find(int[] parents, int value) {
        int root = value;
        while (parents[root] != root) {
            root = parents[root];
        }
        while (parents[value] != value) {
            int next = parents[value];
            parents[value] = root;
            value = next;
        }
        return root;
    }

    private static void union(int[] parents, int first, int second) {
        int rootFirst = find(parents, first);
        int rootSecond = find(parents, second);
        if (rootFirst != rootSecond) {
            parents[rootSecond] = rootFirst;
        }
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static VertexKey vertexKey(TriangleMesh mesh, int triangle, int corner) {
        return new VertexKey(
                quantize(mesh.x(triangle, corner)),
                quantize(mesh.y(triangle, corner)),
                quantize(mesh.z(triangle, corner)));
    }

    private static long quantize(double value) {
        double scaled = value * WELD_SCALE;
        if (scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (scaled <= Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return Math.round(scaled);
    }

    private record VertexKey(long x, long y, long z) {
    }

    private record EdgeKey(VertexKey first, VertexKey second) {
        private static EdgeKey of(VertexKey first, VertexKey second) {
            return compare(first, second) <= 0 ? new EdgeKey(first, second) : new EdgeKey(second, first);
        }

        private static int compare(VertexKey first, VertexKey second) {
            int result = Long.compare(first.x, second.x);
            if (result != 0) {
                return result;
            }
            result = Long.compare(first.y, second.y);
            if (result != 0) {
                return result;
            }
            return Long.compare(first.z, second.z);
        }
    }

    private static final class Region {
        private final int[] triangles;
        private final TriangleMesh mesh;
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;

        private Region(int[] triangles, TriangleMesh mesh) {
            this.triangles = triangles;
            this.mesh = mesh;
            double lowX = Double.POSITIVE_INFINITY;
            double lowY = Double.POSITIVE_INFINITY;
            double lowZ = Double.POSITIVE_INFINITY;
            double highX = Double.NEGATIVE_INFINITY;
            double highY = Double.NEGATIVE_INFINITY;
            double highZ = Double.NEGATIVE_INFINITY;
            for (int triangle : triangles) {
                for (int corner = 0; corner < 3; corner++) {
                    double x = mesh.x(triangle, corner);
                    double y = mesh.y(triangle, corner);
                    double z = mesh.z(triangle, corner);
                    lowX = Math.min(lowX, x);
                    lowY = Math.min(lowY, y);
                    lowZ = Math.min(lowZ, z);
                    highX = Math.max(highX, x);
                    highY = Math.max(highY, y);
                    highZ = Math.max(highZ, z);
                }
            }
            this.minX = lowX;
            this.minY = lowY;
            this.minZ = lowZ;
            this.maxX = highX;
            this.maxY = highY;
            this.maxZ = highZ;
        }

        private double minX() {
            return minX;
        }

        private double minY() {
            return minY;
        }

        private double minZ() {
            return minZ;
        }

        private double maxExtent() {
            return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }

        private double centroid(int axis, int triangle) {
            double total = 0.0;
            for (int corner = 0; corner < 3; corner++) {
                total += axis == 0 ? mesh.x(triangle, corner) : (axis == 1 ? mesh.y(triangle, corner) : mesh.z(triangle, corner));
            }
            return total / 3.0;
        }

        private int uniquePositionCount() {
            Set<Integer> positions = new HashSet<>();
            for (int triangle : triangles) {
                positions.add(mesh.positionIndex(triangle, 0));
                positions.add(mesh.positionIndex(triangle, 1));
                positions.add(mesh.positionIndex(triangle, 2));
            }
            return positions.size();
        }

        private AABB expanded(double padding) {
            double[] x = thicken(minX - padding, maxX + padding);
            double[] y = thicken(minY - padding, maxY + padding);
            double[] z = thicken(minZ - padding, maxZ + padding);
            return new AABB(x[0], y[0], z[0], x[1], y[1], z[1]);
        }

        private static double[] thicken(double minimum, double maximum) {
            if (maximum - minimum >= MIN_REGION_THICKNESS) {
                return new double[]{ minimum, maximum };
            }
            double center = (minimum + maximum) * 0.5;
            double half = MIN_REGION_THICKNESS * 0.5;
            return new double[]{ center - half, center + half };
        }
    }
}
