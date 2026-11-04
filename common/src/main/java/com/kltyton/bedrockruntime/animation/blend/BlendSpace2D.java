package com.kltyton.bedrockruntime.animation.blend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable deterministic two-dimensional blend layout.
 *
 * <p>Layout construction rejects non-finite or duplicate coordinates. Non-collinear layouts are
 * triangulated once using normalized coordinates; collinear layouts use a deterministic line
 * projection. Queries outside the hull clamp to the nearest hull edge.</p>
 *
 * <p>MAE 1.1.4's Delaunay helper is deliberately not reused here: it does not normalize
 * translated/scaled layouts and does not provide the required explicit degenerate-layout
 * behavior. This implementation owns those boundary rules.</p>
 */
public final class BlendSpace2D {
    private static final double EPSILON = 1.0e-10;

    private final float[] xs;
    private final float[] ys;
    private final double[] nx;
    private final double[] ny;
    private final double minX;
    private final double minY;
    private final double span;
    private final boolean collinear;
    private final int[] lineOrder;
    private final int[] hull;
    private final int[][] triangles;

    public BlendSpace2D(float[] xs, float[] ys) {
        Objects.requireNonNull(xs, "xs");
        Objects.requireNonNull(ys, "ys");
        if (xs.length == 0) {
            throw new IllegalArgumentException("Blend space 2D requires at least one sample");
        }
        if (xs.length != ys.length) {
            throw new IllegalArgumentException("Blend space 2D coordinate arrays differ in length: "
                    + xs.length + " and " + ys.length);
        }
        int size = xs.length;
        this.xs = Arrays.copyOf(xs, size);
        this.ys = Arrays.copyOf(ys, size);
        Set<Long> seen = new HashSet<>(Math.max(16, size * 2));
        double localMinX = Double.POSITIVE_INFINITY;
        double localMaxX = Double.NEGATIVE_INFINITY;
        double localMinY = Double.POSITIVE_INFINITY;
        double localMaxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            float x = this.xs[i];
            float y = this.ys[i];
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Blend space 2D sample " + i + " must be finite");
            }
            if (x == 0.0f) {
                x = 0.0f;
                this.xs[i] = 0.0f;
            }
            if (y == 0.0f) {
                y = 0.0f;
                this.ys[i] = 0.0f;
            }
            long key = ((long) Float.floatToIntBits(x) << 32) ^ (Float.floatToIntBits(y) & 0xffffffffL);
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Blend space 2D contains duplicate coordinate ("
                        + x + ", " + y + ")");
            }
            localMinX = Math.min(localMinX, x);
            localMaxX = Math.max(localMaxX, x);
            localMinY = Math.min(localMinY, y);
            localMaxY = Math.max(localMaxY, y);
        }
        this.minX = localMinX;
        this.minY = localMinY;
        this.span = Math.max(localMaxX - localMinX, localMaxY - localMinY);
        this.nx = new double[size];
        this.ny = new double[size];
        double divisor = span <= 0.0 ? 1.0 : span;
        for (int i = 0; i < size; i++) {
            this.nx[i] = (this.xs[i] - minX) / divisor;
            this.ny[i] = (this.ys[i] - minY) / divisor;
        }

        boolean collinearLayout = size <= 2 || isCollinear(this.nx, this.ny);
        int[] localHull = null;
        int[][] localTriangles = null;
        int[] localLineOrder = null;
        if (!collinearLayout) {
            localHull = convexHull(this.nx, this.ny);
            if (localHull.length < 3) {
                collinearLayout = true;
            }
        }
        if (collinearLayout) {
            localLineOrder = buildLineOrder();
        } else {
            localTriangles = triangulate(this.nx, this.ny);
        }
        this.collinear = collinearLayout;
        this.lineOrder = localLineOrder;
        this.hull = localHull;
        this.triangles = localTriangles;
    }

    public static BlendSpace2D of(float[] xs, float[] ys) {
        return new BlendSpace2D(xs, ys);
    }

    public int size() {
        return xs.length;
    }

    public float x(int index) {
        checkIndex(index);
        return xs[index];
    }

    public float y(int index) {
        checkIndex(index);
        return ys[index];
    }

    public boolean isCollinear() {
        return collinear;
    }

    public void evaluate(float x, float y, float[] weights) {
        Objects.requireNonNull(weights, "weights");
        if (weights.length < xs.length) {
            throw new IllegalArgumentException("Blend space 2D weights length " + weights.length
                    + " is smaller than sample count " + xs.length);
        }
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Blend space 2D query must be finite");
        }
        for (int i = 0; i < xs.length; i++) {
            weights[i] = 0.0f;
        }
        if (xs.length == 1) {
            weights[0] = 1.0f;
            return;
        }
        if (collinear) {
            evaluateLine(x, y, weights);
            return;
        }
        double px = (x - minX) / (span <= 0.0 ? 1.0 : span);
        double py = (y - minY) / (span <= 0.0 ? 1.0 : span);
        if (triangles != null) {
            for (int[] triangle : triangles) {
                if (evaluateTriangle(triangle, px, py, weights)) {
                    return;
                }
            }
        }
        clampToHull(px, py, weights);
    }

    public float[] evaluate(float x, float y) {
        float[] weights = new float[xs.length];
        evaluate(x, y, weights);
        return weights;
    }

    private void evaluateLine(float x, float y, float[] weights) {
        boolean useX = xs[lineOrder[lineOrder.length - 1]] - xs[lineOrder[0]]
                >= ys[lineOrder[lineOrder.length - 1]] - ys[lineOrder[0]];
        double value = useX ? x : y;
        int first = lineOrder[0];
        int last = lineOrder[lineOrder.length - 1];
        double firstValue = useX ? xs[first] : ys[first];
        double lastValue = useX ? xs[last] : ys[last];
        if (value <= firstValue) {
            weights[first] = 1.0f;
            return;
        }
        if (value >= lastValue) {
            weights[last] = 1.0f;
            return;
        }
        for (int i = 1; i < lineOrder.length; i++) {
            int left = lineOrder[i - 1];
            int right = lineOrder[i];
            double leftValue = useX ? xs[left] : ys[left];
            double rightValue = useX ? xs[right] : ys[right];
            if (value == leftValue) {
                weights[left] = 1.0f;
                return;
            }
            if (value == rightValue) {
                weights[right] = 1.0f;
                return;
            }
            if (value > leftValue && value < rightValue) {
                double alpha = (value - leftValue) / (rightValue - leftValue);
                weights[left] = (float) (1.0 - alpha);
                weights[right] = (float) alpha;
                return;
            }
        }
        weights[last] = 1.0f;
    }

    private boolean evaluateTriangle(int[] triangle, double px, double py, float[] weights) {
        int a = triangle[0];
        int b = triangle[1];
        int c = triangle[2];
        double ax = nx[a];
        double ay = ny[a];
        double bx = nx[b];
        double by = ny[b];
        double cx = nx[c];
        double cy = ny[c];
        double denominator = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);
        if (Math.abs(denominator) < EPSILON) {
            return false;
        }
        double wa = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / denominator;
        double wb = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / denominator;
        double wc = 1.0 - wa - wb;
        if (wa < -EPSILON || wb < -EPSILON || wc < -EPSILON) {
            return false;
        }
        wa = Math.max(0.0, wa);
        wb = Math.max(0.0, wb);
        wc = Math.max(0.0, wc);
        double sum = wa + wb + wc;
        if (sum <= 0.0) {
            return false;
        }
        weights[a] = (float) (wa / sum);
        weights[b] = (float) (wb / sum);
        weights[c] = (float) (wc / sum);
        return true;
    }

    private void clampToHull(double px, double py, float[] weights) {
        if (hull == null || hull.length < 2) {
            nearestPoint(px, py, weights);
            return;
        }
        int bestA = -1;
        int bestB = -1;
        double bestT = 0.0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < hull.length; i++) {
            int a = hull[i];
            int b = hull[(i + 1) % hull.length];
            double ax = nx[a];
            double ay = ny[a];
            double bx = nx[b];
            double by = ny[b];
            double dx = bx - ax;
            double dy = by - ay;
            double lengthSquared = dx * dx + dy * dy;
            double t;
            if (lengthSquared <= EPSILON) {
                t = 0.0;
            } else {
                t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
                t = Math.max(0.0, Math.min(1.0, t));
            }
            double qx = ax + dx * t;
            double qy = ay + dy * t;
            double distance = (px - qx) * (px - qx) + (py - qy) * (py - qy);
            if (distance < bestDistance - 1.0e-18) {
                bestDistance = distance;
                bestA = a;
                bestB = b;
                bestT = t;
            }
        }
        if (bestA < 0) {
            nearestPoint(px, py, weights);
            return;
        }
        if (bestT <= EPSILON) {
            weights[bestA] = 1.0f;
        } else if (bestT >= 1.0 - EPSILON) {
            weights[bestB] = 1.0f;
        } else {
            weights[bestA] = (float) (1.0 - bestT);
            weights[bestB] = (float) bestT;
        }
    }

    private void nearestPoint(double px, double py, float[] weights) {
        int best = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nx.length; i++) {
            double dx = px - nx[i];
            double dy = py - ny[i];
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance - 1.0e-18) {
                bestDistance = distance;
                best = i;
            }
        }
        weights[best] = 1.0f;
    }

    private int[] buildLineOrder() {
        Integer[] order = new Integer[xs.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        double minNormalizedX = Double.POSITIVE_INFINITY;
        double maxNormalizedX = Double.NEGATIVE_INFINITY;
        double minNormalizedY = Double.POSITIVE_INFINITY;
        double maxNormalizedY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < nx.length; i++) {
            minNormalizedX = Math.min(minNormalizedX, nx[i]);
            maxNormalizedX = Math.max(maxNormalizedX, nx[i]);
            minNormalizedY = Math.min(minNormalizedY, ny[i]);
            maxNormalizedY = Math.max(maxNormalizedY, ny[i]);
        }
        final boolean axisX = maxNormalizedX - minNormalizedX >= maxNormalizedY - minNormalizedY;
        Arrays.sort(order, (a, b) -> {
            int compared = axisX ? Float.compare(xs[a], xs[b]) : Float.compare(ys[a], ys[b]);
            return compared != 0 ? compared : Integer.compare(a, b);
        });
        int[] result = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            result[i] = order[i];
        }
        return result;
    }

    private static int[] convexHull(double[] xs, double[] ys) {
        Integer[] order = new Integer[xs.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            int compared = Double.compare(xs[a], xs[b]);
            if (compared != 0) {
                return compared;
            }
            compared = Double.compare(ys[a], ys[b]);
            return compared != 0 ? compared : Integer.compare(a, b);
        });
        int[] hull = new int[xs.length * 2];
        int k = 0;
        for (int i = 0; i < order.length; i++) {
            int point = order[i];
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], point, xs, ys) <= EPSILON) {
                k--;
            }
            hull[k++] = point;
        }
        int lower = k + 1;
        for (int i = order.length - 2; i >= 0; i--) {
            int point = order[i];
            while (k >= lower && cross(hull[k - 2], hull[k - 1], point, xs, ys) <= EPSILON) {
                k--;
            }
            hull[k++] = point;
        }
        return Arrays.copyOf(hull, Math.max(0, k - 1));
    }

    private static int[][] triangulate(double[] xs, double[] ys) {
        int size = xs.length;
        int total = size + 3;
        double[] tx = Arrays.copyOf(xs, total);
        double[] ty = Arrays.copyOf(ys, total);
        tx[size] = -20.0;
        ty[size] = -20.0;
        tx[size + 1] = 20.0;
        ty[size + 1] = -20.0;
        tx[size + 2] = 0.0;
        ty[size + 2] = 20.0;
        List<int[]> triangles = new ArrayList<>();
        triangles.add(new int[]{size, size + 1, size + 2});
        for (int point = 0; point < size; point++) {
            List<int[]> bad = new ArrayList<>();
            List<int[]> good = new ArrayList<>();
            for (int[] triangle : triangles) {
                if (inCircumcircle(point, triangle, tx, ty)) {
                    bad.add(triangle);
                } else {
                    good.add(triangle);
                }
            }
            if (bad.isEmpty()) {
                splitContaining(triangles, point, tx, ty);
                continue;
            }
            Map<Long, int[]> boundary = new LinkedHashMap<>();
            for (int[] triangle : bad) {
                addBoundaryEdge(boundary, triangle[0], triangle[1]);
                addBoundaryEdge(boundary, triangle[1], triangle[2]);
                addBoundaryEdge(boundary, triangle[2], triangle[0]);
            }
            triangles = good;
            if (boundary.isEmpty()) {
                splitContaining(triangles, point, tx, ty);
                continue;
            }
            for (int[] edge : boundary.values()) {
                triangles.add(new int[]{edge[0], edge[1], point});
            }
        }
        List<int[]> real = new ArrayList<>();
        for (int[] triangle : triangles) {
            if (triangle[0] < size && triangle[1] < size && triangle[2] < size) {
                real.add(triangle);
            }
        }
        return real.toArray(new int[0][]);
    }

    private static void splitContaining(List<int[]> triangles, int point, double[] xs, double[] ys) {
        int index = -1;
        for (int i = 0; i < triangles.size(); i++) {
            if (containsPoint(triangles.get(i), point, xs, ys)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        int[] triangle = triangles.remove(index);
        triangles.add(new int[]{triangle[0], triangle[1], point});
        triangles.add(new int[]{triangle[1], triangle[2], point});
        triangles.add(new int[]{triangle[2], triangle[0], point});
    }

    private static boolean containsPoint(int[] triangle, int point, double[] xs, double[] ys) {
        int a = triangle[0];
        int b = triangle[1];
        int c = triangle[2];
        double denominator = (ys[b] - ys[c]) * (xs[a] - xs[c]) + (xs[c] - xs[b]) * (ys[a] - ys[c]);
        if (Math.abs(denominator) < EPSILON) {
            return false;
        }
        double px = xs[point];
        double py = ys[point];
        double wa = ((ys[b] - ys[c]) * (px - xs[c]) + (xs[c] - xs[b]) * (py - ys[c])) / denominator;
        double wb = ((ys[c] - ys[a]) * (px - xs[c]) + (xs[a] - xs[c]) * (py - ys[c])) / denominator;
        double wc = 1.0 - wa - wb;
        return wa >= -EPSILON && wb >= -EPSILON && wc >= -EPSILON;
    }

    private static void addBoundaryEdge(Map<Long, int[]> edges, int a, int b) {
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        long key = (low << 32) ^ (high & 0xffffffffL);
        if (edges.remove(key) == null) {
            edges.put(key, new int[]{a, b});
        }
    }

    private static boolean inCircumcircle(int point, int[] triangle, double[] xs, double[] ys) {
        int a = triangle[0];
        int b = triangle[1];
        int c = triangle[2];
        if (orient2d(a, b, c, xs, ys) < 0.0) {
            int swap = b;
            b = c;
            c = swap;
        }
        double ax = xs[a] - xs[point];
        double ay = ys[a] - ys[point];
        double bx = xs[b] - xs[point];
        double by = ys[b] - ys[point];
        double cx = xs[c] - xs[point];
        double cy = ys[c] - ys[point];
        double determinant = (ax * ax + ay * ay) * (bx * cy - by * cx)
                - (bx * bx + by * by) * (ax * cy - ay * cx)
                + (cx * cx + cy * cy) * (ax * by - ay * bx);
        return determinant > -EPSILON;
    }

    private static boolean isCollinear(double[] xs, double[] ys) {
        int first = 0;
        int second = 0;
        double bestDistance = -1.0;
        for (int i = 0; i < xs.length; i++) {
            for (int j = i + 1; j < xs.length; j++) {
                double dx = xs[j] - xs[i];
                double dy = ys[j] - ys[i];
                double distance = dx * dx + dy * dy;
                if (distance > bestDistance) {
                    bestDistance = distance;
                    first = i;
                    second = j;
                }
            }
        }
        if (bestDistance <= EPSILON * EPSILON) {
            return true;
        }
        double lineLength = Math.sqrt(bestDistance);
        for (int i = 0; i < xs.length; i++) {
            double cross = (xs[second] - xs[first]) * (ys[i] - ys[first])
                    - (ys[second] - ys[first]) * (xs[i] - xs[first]);
            if (Math.abs(cross) / lineLength > EPSILON) {
                return false;
            }
        }
        return true;
    }

    private static double orient2d(int a, int b, int c, double[] xs, double[] ys) {
        return (xs[b] - xs[a]) * (ys[c] - ys[a]) - (ys[b] - ys[a]) * (xs[c] - xs[a]);
    }

    private static double cross(int a, int b, int c, double[] xs, double[] ys) {
        return (xs[b] - xs[a]) * (ys[c] - ys[a]) - (ys[b] - ys[a]) * (xs[c] - xs[a]);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= xs.length) {
            throw new IllegalArgumentException("Blend space 2D index " + index + " is outside [0, "
                    + xs.length + ")");
        }
    }
}
