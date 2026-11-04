package com.kltyton.bedrockruntime.api.block.shape.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** An authored oriented cuboid, optionally clipped at occupied block-cell boundaries. */
public final class ConvexPart {
    private static final double EPSILON = 1.0E-9;
    private static final double CONTACT_EPSILON = 1.0E-7;
    private static final List<Vec3> WORLD_AXES = List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
    private static final int[][] BOX_FACES = {
            {0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1},
            {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

    private final List<List<Vec3>> faces;
    private final List<Vec3> vertices;
    private final List<Plane> planes;
    private final List<Edge> edges;
    private final List<Projection> boxAxes;
    private final AABB bounds;
    private final boolean axisAligned;

    private ConvexPart(List<List<Vec3>> polygons) {
        faces = polygons.stream().map(List::copyOf).toList();
        List<Vec3> points = new ArrayList<>();
        for (List<Vec3> face : faces) {
            face.forEach(point -> addPoint(points, point));
        }
        vertices = List.copyOf(points);
        Vec3 center = points.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / points.size());
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (Vec3 point : points) {
            minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x); maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
        }
        bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Plane> facePlanes = new ArrayList<>();
        List<Edge> faceEdges = new ArrayList<>();
        List<Vec3> axes = new ArrayList<>(WORLD_AXES);
        for (List<Vec3> face : faces) {
            Vec3 normal = normal(face);
            if (normal.dot(face.get(0).subtract(center)) < 0) {
                normal = normal.scale(-1);
            }
            facePlanes.add(new Plane(normal, normal.dot(face.get(0))));
            addAxis(axes, normal);
            for (int i = 0; i < face.size(); i++) {
                Vec3 from = face.get(i), to = face.get((i + 1) % face.size());
                Edge edge = new Edge(from, to);
                if (from.distanceToSqr(to) > EPSILON * EPSILON && !containsEdge(faceEdges, edge)) {
                    faceEdges.add(edge);
                    for (Vec3 worldAxis : WORLD_AXES) {
                        addAxis(axes, to.subtract(from).cross(worldAxis));
                    }
                }
            }
        }
        planes = List.copyOf(facePlanes);
        edges = List.copyOf(faceEdges);
        boxAxes = axes.stream().map(axis -> project(axis, points)).toList();
        axisAligned = planes.stream().allMatch(plane -> isWorldAxis(plane.normal()));
    }

    public static ConvexPart box(AABB box) {
        return transformedBox(box, UnaryOperator.identity());
    }

    public static ConvexPart transformedBox(AABB box, UnaryOperator<Vec3> transform) {
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = transform.apply(new Vec3((i & 4) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY, (i & 1) == 0 ? box.minZ : box.maxZ));
        }
        List<List<Vec3>> faces = new ArrayList<>(6);
        for (int[] face : BOX_FACES) {
            faces.add(List.of(corners[face[0]], corners[face[1]], corners[face[2]], corners[face[3]]));
        }
        return new ConvexPart(faces);
    }

    public AABB bounds() { return bounds; }
    public List<Edge> edges() { return edges; }
    public List<Vec3> vertices() { return vertices; }
    public boolean isAxisAligned() { return axisAligned; }

    public ConvexPart transform(UnaryOperator<Vec3> transform) {
        return new ConvexPart(faces.stream().map(face -> face.stream().map(transform).toList()).toList());
    }

    public ConvexPart move(Vec3 offset) {
        return offset.equals(Vec3.ZERO) ? this
                : new ConvexPart(faces.stream().map(face -> face.stream().map(point -> point.add(offset)).toList()).toList());
    }

    public boolean contains(Vec3 point) {
        return planes.stream().allMatch(plane -> plane.distance(point) <= EPSILON);
    }

    public @Nullable RayHit clipRay(Vec3 from, Vec3 to) {
        Vec3 movement = to.subtract(from);
        double entry = 0, exit = 1;
        Vec3 hitNormal = unit(movement).scale(-1);
        boolean inside = contains(from);
        for (Plane plane : planes) {
            double distance = plane.distance(from);
            double speed = plane.normal().dot(movement);
            if (Math.abs(speed) <= EPSILON) {
                if (distance > EPSILON) { return null; }
                continue;
            }
            double time = -distance / speed;
            if (speed < 0 && time > entry) {
                entry = time;
                hitNormal = plane.normal();
            } else if (speed > 0) {
                exit = Math.min(exit, time);
            }
            if (entry > exit + EPSILON) { return null; }
        }
        return entry <= 1 && exit >= 0 ? new RayHit(Math.max(0, entry), hitNormal, inside) : null;
    }

    /** Continuous SAT for a translating entity AABB; touching while moving away is not a new collision. */
    public @Nullable SweepHit sweep(AABB moving, Vec3 movement) {
        if (!bounds.inflate(CONTACT_EPSILON).intersects(moving.expandTowards(movement))) { return null; }
        double entry = Double.NEGATIVE_INFINITY, exit = Double.POSITIVE_INFINITY;
        Vec3 hitNormal = Vec3.ZERO;
        for (Projection projection : boxAxes) {
            Vec3 axis = projection.axis();
            double min = projectMin(moving, axis), max = projectMax(moving, axis);
            double speed = movement.dot(axis);
            if (Math.abs(speed) <= EPSILON) {
                if (max <= projection.min() + EPSILON || min >= projection.max() - EPSILON) { return null; }
                continue;
            }
            double near = (projection.min() - max) / speed;
            double far = (projection.max() - min) / speed;
            if (near > far) { double swap = near; near = far; far = swap; }
            if (near > entry) {
                entry = near;
                hitNormal = axis.scale(speed > 0 ? -1 : 1);
            }
            exit = Math.min(exit, far);
            if (entry > exit + EPSILON) { return null; }
        }
        // Entity widths originate as floats; contact tolerance is a distance, not a speed-dependent sweep time.
        double inwardSpeed = -movement.dot(hitNormal);
        if (-entry * inwardSpeed > CONTACT_EPSILON || entry > 1 || exit < 0 || inwardSpeed <= EPSILON) { return null; }
        return new SweepHit(Math.max(0, entry), hitNormal);
    }

    public boolean intersects(AABB box) {
        if (!bounds.intersects(box)) { return false; }
        for (Projection projection : boxAxes) {
            if (projectMax(box, projection.axis()) <= projection.min() + EPSILON
                    || projectMin(box, projection.axis()) >= projection.max() - EPSILON) { return false; }
        }
        return true;
    }

    public boolean intersects(ConvexPart other) {
        if (!bounds.intersects(other.bounds)) { return false; }
        for (Plane plane : planes) {
            if (separated(plane.normal(), other)) { return false; }
        }
        for (Plane plane : other.planes) {
            if (separated(plane.normal(), other)) { return false; }
        }
        for (Edge first : edges) {
            for (Edge second : other.edges) {
                Vec3 axis = first.to().subtract(first.from()).cross(second.to().subtract(second.from()));
                if (axis.lengthSqr() > EPSILON * EPSILON && separated(unit(axis), other)) { return false; }
            }
        }
        return true;
    }

    private boolean separated(Vec3 axis, ConvexPart other) {
        Projection first = project(axis, vertices), second = project(axis, other.vertices);
        return first.max() <= second.min() + EPSILON || second.max() <= first.min() + EPSILON;
    }

    public @Nullable ConvexPart intersect(AABB box) {
        if (!intersects(box)) { return null; }
        ConvexPart result = this;
        for (Plane plane : box(box).planes) {
            result = result.clip(plane);
            if (result == null) { return null; }
        }
        return result;
    }

    public @Nullable ConvexPart intersect(ConvexPart other) {
        if (!intersects(other)) { return null; }
        ConvexPart result = this;
        for (Plane plane : other.planes) {
            result = result.clip(plane);
            if (result == null) { return null; }
        }
        return result;
    }

    public List<ConvexPart> subtract(ConvexPart other) {
        if (!intersects(other)) { return List.of(this); }
        List<ConvexPart> outside = new ArrayList<>();
        ConvexPart remainder = this;
        for (Plane plane : other.planes) {
            ConvexPart piece = remainder.clip(plane.opposite());
            if (piece != null) { outside.add(piece); }
            remainder = remainder.clip(plane);
            if (remainder == null) { break; }
        }
        return outside;
    }

    private @Nullable ConvexPart clip(Plane plane) {
        boolean outside = false, inside = false;
        for (Vec3 point : vertices) {
            double distance = plane.distance(point);
            outside |= distance > EPSILON;
            inside |= distance < -EPSILON;
        }
        if (!outside) { return this; }
        if (!inside) { return null; }
        List<List<Vec3>> clippedFaces = new ArrayList<>();
        List<Vec3> cap = new ArrayList<>();
        for (List<Vec3> face : faces) {
            List<Vec3> clipped = new ArrayList<>();
            for (int i = 0; i < face.size(); i++) {
                Vec3 a = face.get(i), b = face.get((i + 1) % face.size());
                double da = plane.distance(a), db = plane.distance(b);
                boolean keepA = da <= EPSILON, keepB = db <= EPSILON;
                if (keepA) { addPoint(clipped, a); }
                if (keepA != keepB) {
                    Vec3 crossing = a.add(b.subtract(a).scale(da / (da - db)));
                    addPoint(clipped, crossing);
                    addPoint(cap, crossing);
                } else if (Math.abs(da) <= EPSILON) {
                    addPoint(cap, a);
                }
            }
            if (clipped.size() >= 3 && normal(clipped).lengthSqr() > EPSILON) { clippedFaces.add(clipped); }
        }
        if (cap.size() >= 3) { clippedFaces.add(sortOnPlane(cap, plane.normal())); }
        return clippedFaces.size() >= 4 ? new ConvexPart(clippedFaces) : null;
    }

    /** Unit-depth extrusion of the exact cross-section used by vanilla face-support queries. */
    public @Nullable ConvexPart faceSection(Direction direction) {
        Direction.Axis axis = direction.getAxis();
        double coordinate = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        List<Vec3> section = new ArrayList<>();
        for (Edge edge : edges) {
            double first = edge.from().get(axis) - coordinate, second = edge.to().get(axis) - coordinate;
            if (Math.abs(first) <= EPSILON) { addPoint(section, edge.from()); }
            if (Math.abs(second) <= EPSILON) { addPoint(section, edge.to()); }
            if (first * second < -EPSILON * EPSILON) {
                addPoint(section, edge.from().add(edge.to().subtract(edge.from()).scale(first / (first - second))));
            }
        }
        if (section.size() < 3) { return null; }
        List<Vec3> ordered = sortOnPlane(section, WORLD_AXES.get(axis.ordinal()));
        List<Vec3> front = ordered.stream().map(point -> point.with(axis, 0)).toList();
        List<Vec3> back = ordered.stream().map(point -> point.with(axis, 1)).toList();
        List<List<Vec3>> extrusion = new ArrayList<>();
        extrusion.add(front); extrusion.add(back);
        for (int i = 0; i < front.size(); i++) {
            int next = (i + 1) % front.size();
            extrusion.add(List.of(front.get(i), front.get(next), back.get(next), back.get(i)));
        }
        return new ConvexPart(extrusion);
    }

    public Vec3 closestPoint(Vec3 point) {
        if (contains(point)) { return point; }
        Vec3 closest = vertices.get(0);
        for (List<Vec3> face : faces) {
            for (int i = 1; i + 1 < face.size(); i++) {
                Vec3 candidate = closestOnTriangle(point, face.get(0), face.get(i), face.get(i + 1));
                if (candidate.distanceToSqr(point) < closest.distanceToSqr(point)) { closest = candidate; }
            }
        }
        return closest;
    }

    private static Vec3 closestOnTriangle(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 ab = b.subtract(a), ac = c.subtract(a), ap = p.subtract(a);
        double d1 = ab.dot(ap), d2 = ac.dot(ap);
        if (d1 <= 0 && d2 <= 0) { return a; }
        Vec3 bp = p.subtract(b);
        double d3 = ab.dot(bp), d4 = ac.dot(bp);
        if (d3 >= 0 && d4 <= d3) { return b; }
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0) { return a.add(ab.scale(d1 / (d1 - d3))); }
        Vec3 cp = p.subtract(c);
        double d5 = ab.dot(cp), d6 = ac.dot(cp);
        if (d6 >= 0 && d5 <= d6) { return c; }
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0) { return a.add(ac.scale(d2 / (d2 - d6))); }
        double va = d3 * d6 - d5 * d4;
        if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) { return b.add(c.subtract(b).scale((d4 - d3) / ((d4 - d3) + (d5 - d6)))); }
        double scale = 1.0 / (va + vb + vc);
        return a.add(ab.scale(vb * scale)).add(ac.scale(vc * scale));
    }

    private static List<Vec3> sortOnPlane(List<Vec3> points, Vec3 normal) {
        Vec3 center = points.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / points.size());
        Vec3 u = unit(normal.cross(Math.abs(normal.x) < 0.8 ? WORLD_AXES.get(0) : WORLD_AXES.get(1)));
        Vec3 v = normal.cross(u);
        return points.stream().sorted(Comparator.comparingDouble(point -> {
            Vec3 delta = point.subtract(center);
            return Math.atan2(delta.dot(v), delta.dot(u));
        })).toList();
    }

    private static Vec3 normal(List<Vec3> face) {
        Vec3 first = face.get(1).subtract(face.get(0));
        for (int i = 2; i < face.size(); i++) {
            Vec3 cross = first.cross(face.get(i).subtract(face.get(0)));
            if (cross.lengthSqr() > EPSILON * EPSILON) { return unit(cross); }
        }
        return Vec3.ZERO;
    }

    private static Vec3 unit(Vec3 value) {
        double length = value.length();
        return length <= EPSILON ? Vec3.ZERO : value.scale(1.0 / length);
    }

    private static void addPoint(List<Vec3> points, Vec3 point) {
        if (points.stream().noneMatch(existing -> existing.distanceToSqr(point) <= EPSILON * EPSILON)) { points.add(point); }
    }

    private static boolean containsEdge(List<Edge> edges, Edge edge) {
        return edges.stream().anyMatch(existing ->
                samePoint(existing.from(), edge.from()) && samePoint(existing.to(), edge.to())
                        || samePoint(existing.from(), edge.to()) && samePoint(existing.to(), edge.from()));
    }

    private static boolean samePoint(Vec3 a, Vec3 b) { return a.distanceToSqr(b) <= EPSILON * EPSILON; }

    private static void addAxis(List<Vec3> axes, Vec3 axis) {
        if (axis.lengthSqr() <= EPSILON * EPSILON) { return; }
        Vec3 unit = unit(axis);
        if (axes.stream().noneMatch(existing -> Math.abs(existing.dot(unit)) > 1 - EPSILON)) { axes.add(unit); }
    }

    private static boolean isWorldAxis(Vec3 axis) {
        return Math.max(Math.abs(axis.x), Math.max(Math.abs(axis.y), Math.abs(axis.z))) > 1 - EPSILON;
    }

    private static Projection project(Vec3 axis, List<Vec3> points) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (Vec3 point : points) {
            double value = axis.dot(point);
            min = Math.min(min, value); max = Math.max(max, value);
        }
        return new Projection(axis, min, max);
    }

    private static double projectMin(AABB box, Vec3 axis) {
        return component(axis.x, box.minX, box.maxX) + component(axis.y, box.minY, box.maxY) + component(axis.z, box.minZ, box.maxZ);
    }

    private static double projectMax(AABB box, Vec3 axis) { return -projectMin(box, axis.scale(-1)); }
    private static double component(double axis, double min, double max) { return axis == 0 ? 0 : axis * (axis > 0 ? min : max); }

    public record Edge(Vec3 from, Vec3 to) { }
    public record RayHit(double time, Vec3 normal, boolean inside) { }
    public record SweepHit(double time, Vec3 normal) { }
    private record Projection(Vec3 axis, double min, double max) { }
    private record Plane(Vec3 normal, double offset) {
        double distance(Vec3 point) { return normal.dot(point) - offset; }
        Plane opposite() { return new Plane(normal.scale(-1), -offset); }
    }
}
