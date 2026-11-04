package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3d;

final class MeshPolygonTriangulator {
    private static final double DUPLICATE_EPSILON_SQUARED = 1.0E-14;
    private static final double PLANAR_ABSOLUTE_EPSILON = 1.0E-7;
    private static final double PLANAR_RELATIVE_EPSILON = 1.0E-5;
    private static final double INTERSECTION_EPSILON = 1.0E-12;
    private static final double CONVEX_EPSILON = 1.0E-14;
    private static final int MAX_POLYGON_VERTICES = 1024;

    private MeshPolygonTriangulator() {
    }

    static int[][] triangulate(List<Vector3d> input) {
        List<Vector3d> points = removeDuplicates(input);
        if (points.size() < 3) {
            throw new IllegalArgumentException("polygon has fewer than three distinct vertices");
        }
        if (points.size() > MAX_POLYGON_VERTICES) {
            throw new IllegalArgumentException("polygon exceeds " + MAX_POLYGON_VERTICES + " vertices");
        }
        Vector3d normal = newellNormal(points);
        double normalLength = normal.length();
        if (!Double.isFinite(normalLength) || normalLength <= 1.0E-12) {
            throw new IllegalArgumentException("polygon has no stable plane normal");
        }
        normal.div(normalLength);
        double maximumExtent = maximumExtent(points);
        double tolerance = Math.max(PLANAR_ABSOLUTE_EPSILON, maximumExtent * PLANAR_RELATIVE_EPSILON);
        Vector3d origin = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            Vector3d offset = new Vector3d(points.get(i)).sub(origin);
            if (Math.abs(offset.dot(normal)) > tolerance) {
                throw new IllegalArgumentException("polygon is not planar");
            }
        }
        Vector3d u = basisU(points, normal);
        Vector3d v = new Vector3d(normal).cross(u).normalize();
        int count = points.size();
        double[] xs = new double[count];
        double[] ys = new double[count];
        project(points, origin, u, v, xs, ys);
        List<Integer> order = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            order.add(i);
        }
        removeCollinear(order, xs, ys);
        if (order.size() < 3) {
            throw new IllegalArgumentException("polygon has no surface area");
        }
        double area = signedArea(order, xs, ys);
        if (Math.abs(area) <= 1.0E-14) {
            throw new IllegalArgumentException("polygon has zero area");
        }
        if (area < 0.0) {
            for (int i = 0; i < ys.length; i++) {
                ys[i] = -ys[i];
            }
        }
        checkDuplicateVertices(order, xs, ys);
        checkSelfIntersections(order, xs, ys);
        return earClip(order, xs, ys);
    }

    private static List<Vector3d> removeDuplicates(List<Vector3d> input) {
        List<Vector3d> result = new ArrayList<>(input.size());
        for (Vector3d point : input) {
            if (point == null || !finite(point)) {
                throw new IllegalArgumentException("polygon vertex is missing or non-finite");
            }
            if (result.isEmpty() || result.get(result.size() - 1).distanceSquared(point) > DUPLICATE_EPSILON_SQUARED) {
                result.add(new Vector3d(point));
            }
        }
        if (result.size() > 1 && result.get(0).distanceSquared(result.get(result.size() - 1)) <= DUPLICATE_EPSILON_SQUARED) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static Vector3d newellNormal(List<Vector3d> points) {
        Vector3d normal = new Vector3d();
        for (int i = 0; i < points.size(); i++) {
            Vector3d current = points.get(i);
            Vector3d next = points.get((i + 1) % points.size());
            normal.x += (current.y - next.y) * (current.z + next.z);
            normal.y += (current.z - next.z) * (current.x + next.x);
            normal.z += (current.x - next.x) * (current.y + next.y);
        }
        return normal;
    }

    private static double maximumExtent(List<Vector3d> points) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vector3d point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
    }

    private static Vector3d basisU(List<Vector3d> points, Vector3d normal) {
        Vector3d first = points.get(0);
        Vector3d best = null;
        double bestLength = 0.0;
        for (int i = 1; i < points.size(); i++) {
            Vector3d edge = new Vector3d(points.get(i)).sub(first);
            double length = edge.lengthSquared();
            if (length > bestLength && length > 1.0E-20) {
                Vector3d candidate = new Vector3d(edge).cross(normal);
                if (candidate.lengthSquared() > 1.0E-20) {
                    best = edge;
                    bestLength = length;
                }
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("polygon has no usable edge");
        }
        return best.normalize();
    }

    private static void project(List<Vector3d> points, Vector3d origin, Vector3d u, Vector3d v,
                                double[] xs, double[] ys) {
        for (int i = 0; i < points.size(); i++) {
            Vector3d offset = new Vector3d(points.get(i)).sub(origin);
            xs[i] = offset.dot(u);
            ys[i] = offset.dot(v);
        }
    }

    private static void removeCollinear(List<Integer> order, double[] xs, double[] ys) {
        boolean removed = true;
        while (removed && order.size() > 3) {
            removed = false;
            int size = order.size();
            for (int i = 0; i < size; i++) {
                int previous = order.get((i + size - 1) % size);
                int current = order.get(i);
                int next = order.get((i + 1) % size);
                double cross = cross(xs[previous], ys[previous], xs[current], ys[current], xs[next], ys[next]);
                double lengthProduct = Math.sqrt(distanceSquared(xs[previous], ys[previous], xs[current], ys[current])
                        * distanceSquared(xs[current], ys[current], xs[next], ys[next]));
                if (Math.abs(cross) <= 1.0E-10 * Math.max(1.0, lengthProduct)) {
                    double dot = (xs[current] - xs[previous]) * (xs[next] - xs[current])
                            + (ys[current] - ys[previous]) * (ys[next] - ys[current]);
                    if (dot >= 0.0) {
                        order.remove(i);
                        removed = true;
                        break;
                    }
                }
            }
        }
    }

    private static void checkDuplicateVertices(List<Integer> order, double[] xs, double[] ys) {
        for (int i = 0; i < order.size(); i++) {
            int first = order.get(i);
            for (int j = i + 1; j < order.size(); j++) {
                if (j == i + 1 || (i == 0 && j == order.size() - 1)) {
                    continue;
                }
                int second = order.get(j);
                if (distanceSquared(xs[first], ys[first], xs[second], ys[second]) <= DUPLICATE_EPSILON_SQUARED) {
                    throw new IllegalArgumentException("polygon touches a non-adjacent vertex");
                }
            }
        }
    }

    private static void checkSelfIntersections(List<Integer> order, double[] xs, double[] ys) {
        int count = order.size();
        for (int i = 0; i < count; i++) {
            int a = order.get(i);
            int b = order.get((i + 1) % count);
            for (int j = i + 1; j < count; j++) {
                if (j == i || j == (i + 1) % count || (j + 1) % count == i) {
                    continue;
                }
                int c = order.get(j);
                int d = order.get((j + 1) % count);
                if (segmentsIntersect(xs[a], ys[a], xs[b], ys[b], xs[c], ys[c], xs[d], ys[d])) {
                    throw new IllegalArgumentException("polygon self-intersects");
                }
            }
        }
    }

    private static int[][] earClip(List<Integer> order, double[] xs, double[] ys) {
        List<Integer> remaining = new ArrayList<>(order);
        List<int[]> triangles = new ArrayList<>(order.size() - 2);
        int guard = order.size() * order.size();
        while (remaining.size() > 3) {
            boolean clipped = false;
            int size = remaining.size();
            for (int position = 0; position < size; position++) {
                int previous = remaining.get((position + size - 1) % size);
                int current = remaining.get(position);
                int next = remaining.get((position + 1) % size);
                if (cross(xs[previous], ys[previous], xs[current], ys[current], xs[next], ys[next]) <= CONVEX_EPSILON) {
                    continue;
                }
                boolean blocked = false;
                for (int other = 0; other < size; other++) {
                    int candidate = remaining.get(other);
                    if (candidate == previous || candidate == current || candidate == next) {
                        continue;
                    }
                    if (pointInTriangle(xs[candidate], ys[candidate],
                            xs[previous], ys[previous], xs[current], ys[current], xs[next], ys[next])) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) {
                    continue;
                }
                triangles.add(new int[]{ previous, current, next });
                remaining.remove(position);
                clipped = true;
                break;
            }
            if (!clipped || guard-- <= 0) {
                throw new IllegalArgumentException("polygon cannot be triangulated; it may self-intersect or be degenerate");
            }
        }
        if (remaining.size() != 3) {
            throw new IllegalArgumentException("polygon triangulation did not terminate");
        }
        triangles.add(new int[]{ remaining.get(0), remaining.get(1), remaining.get(2) });
        return triangles.toArray(new int[0][]);
    }

    private static boolean pointInTriangle(double px, double py,
                                           double ax, double ay, double bx, double by, double cx, double cy) {
        double first = cross(ax, ay, bx, by, px, py);
        double second = cross(bx, by, cx, cy, px, py);
        double third = cross(cx, cy, ax, ay, px, py);
        return first >= -INTERSECTION_EPSILON && second >= -INTERSECTION_EPSILON && third >= -INTERSECTION_EPSILON;
    }

    private static boolean segmentsIntersect(double ax, double ay, double bx, double by,
                                             double cx, double cy, double dx, double dy) {
        double first = cross(ax, ay, bx, by, cx, cy);
        double second = cross(ax, ay, bx, by, dx, dy);
        double third = cross(cx, cy, dx, dy, ax, ay);
        double fourth = cross(cx, cy, dx, dy, bx, by);
        if (((first > INTERSECTION_EPSILON && second < -INTERSECTION_EPSILON)
                || (first < -INTERSECTION_EPSILON && second > INTERSECTION_EPSILON))
                && ((third > INTERSECTION_EPSILON && fourth < -INTERSECTION_EPSILON)
                || (third < -INTERSECTION_EPSILON && fourth > INTERSECTION_EPSILON))) {
            return true;
        }
        if (Math.abs(first) <= INTERSECTION_EPSILON && onSegment(ax, ay, bx, by, cx, cy)) {
            return true;
        }
        if (Math.abs(second) <= INTERSECTION_EPSILON && onSegment(ax, ay, bx, by, dx, dy)) {
            return true;
        }
        if (Math.abs(third) <= INTERSECTION_EPSILON && onSegment(cx, cy, dx, dy, ax, ay)) {
            return true;
        }
        return Math.abs(fourth) <= INTERSECTION_EPSILON && onSegment(cx, cy, dx, dy, bx, by);
    }

    private static boolean onSegment(double ax, double ay, double bx, double by, double px, double py) {
        return px >= Math.min(ax, bx) - INTERSECTION_EPSILON
                && px <= Math.max(ax, bx) + INTERSECTION_EPSILON
                && py >= Math.min(ay, by) - INTERSECTION_EPSILON
                && py <= Math.max(ay, by) + INTERSECTION_EPSILON;
    }

    private static double signedArea(List<Integer> order, double[] xs, double[] ys) {
        double area = 0.0;
        for (int i = 0; i < order.size(); i++) {
            int current = order.get(i);
            int next = order.get((i + 1) % order.size());
            area += xs[current] * ys[next] - xs[next] * ys[current];
        }
        return area * 0.5;
    }

    private static double cross(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static double distanceSquared(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static boolean finite(Vector3d point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
