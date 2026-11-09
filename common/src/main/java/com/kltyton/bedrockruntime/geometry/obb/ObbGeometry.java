package com.kltyton.bedrockruntime.geometry.obb;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.MeshRayQuery;

/**
 * CN: loader/逻辑端无关的 OBB 几何快照与 SAT 相交计算。
 * EN: Loader- and logical-side-neutral OBB geometry snapshot with SAT intersection tests.
 */
public record ObbGeometry(AABB localBounds, AABB worldBounds, Matrix4f localToWorld, Matrix4f worldToLocal,
                          TriangleMesh mesh) {
    private static final double PARALLEL_EPSILON = 1.0E-7;
    private static final double SAT_EPSILON = 1.0E-6;
    private static final double SWEEP_EPSILON = 1.0E-7;

    public ObbGeometry(AABB localBounds, AABB worldBounds, Matrix4f localToWorld, Matrix4f worldToLocal) {
        this(localBounds, worldBounds, localToWorld, worldToLocal, null);
    }

    public ObbGeometry withMesh(TriangleMesh mesh) {
        return new ObbGeometry(localBounds, worldBounds, localToWorld, worldToLocal, Objects.requireNonNull(mesh));
    }

    public ObbGeometry {
        Objects.requireNonNull(localBounds, "localBounds");
        Objects.requireNonNull(worldBounds, "worldBounds");
        Objects.requireNonNull(localToWorld, "localToWorld");
        Objects.requireNonNull(worldToLocal, "worldToLocal");
        localToWorld = new Matrix4f(localToWorld);
        worldToLocal = new Matrix4f(worldToLocal);
    }

    /** CN: 返回防御性复制，调用方无法修改快照内部矩阵。EN: Returns a defensive copy so callers cannot mutate this snapshot. */
    @Override
    public Matrix4f localToWorld() {
        return new Matrix4f(localToWorld);
    }

    /** CN: 返回防御性复制，调用方无法修改快照内部矩阵。EN: Returns a defensive copy so callers cannot mutate this snapshot. */
    @Override
    public Matrix4f worldToLocal() {
        return new Matrix4f(worldToLocal);
    }

    /** CN: OBB 的本地空间中心。EN: Center of this OBB in local space. */
    public Vec3 localCenter() {
        return new Vec3(
                (localBounds.minX + localBounds.maxX) * 0.5,
                (localBounds.minY + localBounds.maxY) * 0.5,
                (localBounds.minZ + localBounds.maxZ) * 0.5);
    }

    /** CN: OBB 的世界空间中心。EN: Center of this OBB in world space. */
    public Vec3 worldCenter() {
        ObbProjection projection = ObbProjection.of(this);
        return vec3(projection.center);
    }

    /**
     * CN: 变换后归一化的本地 X/Y/Z 轴；含剪切的仿射变换不保证三轴正交。
     * EN: Normalized transformed local X/Y/Z axes; affine shear can make them non-orthogonal.
     */
    public List<Vec3> worldAxes() {
        ObbProjection projection = ObbProjection.of(this);
        return List.of(vec3(projection.axisX), vec3(projection.axisY), vec3(projection.axisZ));
    }

    /** CN: 经模型缩放后的三个世界空间半尺寸。EN: Three world-space half extents after model scaling. */
    public Vec3 worldHalfExtents() {
        ObbProjection projection = ObbProjection.of(this);
        return new Vec3(projection.extentX, projection.extentY, projection.extentZ);
    }

    /**
     * CN: 按二进制角点顺序返回本地空间的 8 个顶点，列表不可修改。
     * EN: Returns the eight local-space vertices in binary-corner order as an immutable list.
     */
    public List<Vec3> localVertices() {
        return vertices(new Matrix4f());
    }

    /**
     * CN: 按与 {@link #localVertices()} 相同的顺序返回世界空间的 8 个顶点。
     * EN: Returns the eight world-space vertices in the same order as {@link #localVertices()}.
     */
    public List<Vec3> worldVertices() {
        return vertices(localToWorld);
    }

    /** CN: 将本地点变换到世界空间。EN: Transforms a local-space point into world space. */
    public Vec3 toWorldPosition(Vec3 localPosition) {
        Objects.requireNonNull(localPosition, "localPosition");
        Vector3f result = localToWorld.transformPosition(
                (float) localPosition.x(), (float) localPosition.y(), (float) localPosition.z(), new Vector3f());
        return vec3(result);
    }

    /** CN: 将世界点变换到 OBB 本地空间。EN: Transforms a world-space point into OBB local space. */
    public Vec3 toLocalPosition(Vec3 worldPosition) {
        Objects.requireNonNull(worldPosition, "worldPosition");
        Vector3f result = worldToLocal.transformPosition(
                (float) worldPosition.x(), (float) worldPosition.y(), (float) worldPosition.z(), new Vector3f());
        return vec3(result);
    }

    /** CN: 将本地方向变换到世界空间，不执行归一化。EN: Transforms a local direction into world space without normalization. */
    public Vec3 toWorldDirection(Vec3 localDirection) {
        Objects.requireNonNull(localDirection, "localDirection");
        Vector3f result = localToWorld.transformDirection(
                (float) localDirection.x(), (float) localDirection.y(), (float) localDirection.z(), new Vector3f());
        return vec3(result);
    }

    /** CN: 将世界方向变换到 OBB 本地空间，不执行归一化。EN: Transforms a world direction into OBB local space without normalization. */
    public Vec3 toLocalDirection(Vec3 worldDirection) {
        Objects.requireNonNull(worldDirection, "worldDirection");
        Vector3f result = worldToLocal.transformDirection(
                (float) worldDirection.x(), (float) worldDirection.y(), (float) worldDirection.z(), new Vector3f());
        return vec3(result);
    }

    /** CN: 从本地长方体和世界仿射变换创建几何快照。EN: Creates box geometry from local bounds and a world affine transform. */
    public static ObbGeometry fromLocalBounds(AABB localBounds, Matrix4f localToWorld) {
        Matrix4f transform = new Matrix4f(localToWorld);
        Matrix4f inverse = new Matrix4f(transform).invert();
        Vector3f point = new Vector3f();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    transform.transformPosition(
                            (float) (x == 0 ? localBounds.minX : localBounds.maxX),
                            (float) (y == 0 ? localBounds.minY : localBounds.maxY),
                            (float) (z == 0 ? localBounds.minZ : localBounds.maxZ),
                            point);
                    minX = Math.min(minX, point.x());
                    minY = Math.min(minY, point.y());
                    minZ = Math.min(minZ, point.z());
                    maxX = Math.max(maxX, point.x());
                    maxY = Math.max(maxY, point.y());
                    maxZ = Math.max(maxZ, point.z());
                }
            }
        }
        return new ObbGeometry(localBounds, new AABB(minX, minY, minZ, maxX, maxY, maxZ), transform, inverse);
    }

    /** CN: 在世界空间平移该 OBB 快照。EN: Translates this OBB snapshot in world space. */
    public ObbGeometry translated(Vec3 movement) {
        if (movement.lengthSqr() <= SWEEP_EPSILON * SWEEP_EPSILON) {
            return this;
        }
        Matrix4f transform = new Matrix4f()
                .translation((float) movement.x(), (float) movement.y(), (float) movement.z())
                .mul(localToWorld);
        return new ObbGeometry(
                localBounds,
                worldBounds.move(movement),
                transform,
                new Matrix4f(transform).invert(), mesh);
    }

    public double clipDistanceSqr(Vec3 from, Vec3 to) {
        if (!worldBounds.contains(from) && worldBounds.clip(from, to).isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double hitT;
        if (mesh == null) {
            Vector3f localFrom = worldToLocal.transformPosition((float) from.x(), (float) from.y(), (float) from.z(), new Vector3f());
            Vector3f localTo = worldToLocal.transformPosition((float) to.x(), (float) to.y(), (float) to.z(), new Vector3f());
            hitT = clipLocal(localFrom, localTo);
        } else {
            hitT = MeshRayQuery.clipFraction(mesh, localBounds, worldToLocal, from, to);
        }
        if (!Double.isFinite(hitT)) {
            return Double.POSITIVE_INFINITY;
        }
        return to.subtract(from).lengthSqr() * hitT * hitT;
    }

    /**
     * CN: 用竖直线段查询 OBB 最先命中的真实表面；命中高度来自 OBB 面而不是包围它的世界 AABB。
     * EN: Queries the first true OBB surface hit by a vertical segment; the height comes from the OBB face, not its enclosing world AABB.
     */
    public Optional<SurfaceHit> verticalSurfaceAt(double x, double z, double fromY, double toY) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(fromY) || !Double.isFinite(toY)
                || fromY <= toY
                || x < worldBounds.minX - SAT_EPSILON || x > worldBounds.maxX + SAT_EPSILON
                || z < worldBounds.minZ - SAT_EPSILON || z > worldBounds.maxZ + SAT_EPSILON) {
            return Optional.empty();
        }

        Vector3f localFrom = worldToLocal.transformPosition((float) x, (float) fromY, (float) z, new Vector3f());
        Vector3f localTo = worldToLocal.transformPosition((float) x, (float) toY, (float) z, new Vector3f());
        ClipRange range = new ClipRange();
        if (!clipAxis(localFrom.x(), localTo.x() - localFrom.x(), localBounds.minX, localBounds.maxX, 0, range)
                || !clipAxis(localFrom.y(), localTo.y() - localFrom.y(), localBounds.minY, localBounds.maxY, 1, range)
                || !clipAxis(localFrom.z(), localTo.z() - localFrom.z(), localBounds.minZ, localBounds.maxZ, 2, range)
                || range.entryAxis < 0) {
            return Optional.empty();
        }

        Vector3f localNormal = switch (range.entryAxis) {
            case 0 -> new Vector3f(range.entrySign, 0.0F, 0.0F);
            case 1 -> new Vector3f(0.0F, range.entrySign, 0.0F);
            case 2 -> new Vector3f(0.0F, 0.0F, range.entrySign);
            default -> throw new IllegalStateException("Invalid OBB entry axis: " + range.entryAxis);
        };
        Vector3f worldNormal = localToWorld.transformDirection(localNormal, new Vector3f());
        if (worldNormal.lengthSquared() <= 1.0E-12F) {
            return Optional.empty();
        }
        worldNormal.normalize();
        double hitY = fromY + (toY - fromY) * range.near;
        return Optional.of(new SurfaceHit(
                new Vec3(x, hitY, z),
                new Vec3(worldNormal.x(), worldNormal.y(), worldNormal.z())));
    }

    public Optional<Overlap> overlap(ObbGeometry other) {
        if (!worldBounds.intersects(other.worldBounds)) {
            return Optional.empty();
        }
        ObbProjection first = ObbProjection.of(this);
        ObbProjection second = ObbProjection.of(other);
        Vector3f centerDelta = new Vector3f(first.center()).sub(second.center());
        OverlapTracker tracker = new OverlapTracker(centerDelta);

        for (int i = 0; i < 3; i++) {
            Vector3f axis = first.faceNormal(i);
            if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis)
                    - Math.abs(centerDelta.dot(axis)))) {
                return Optional.empty();
            }
        }

        for (int i = 0; i < 3; i++) {
            Vector3f axis = second.faceNormal(i);
            if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis)
                    - Math.abs(centerDelta.dot(axis)))) {
                return Optional.empty();
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector3f axis = crossAxis(first.axis(i), second.axis(j));
                if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis)
                        - Math.abs(centerDelta.dot(axis)))) {
                    return Optional.empty();
                }
            }
        }

        return Optional.ofNullable(tracker.result());
    }

    /**
     * CN: 对仅发生平移的第一个 OBB 执行连续 SAT，返回 [0,1] 内最早接触时间和朝外法线。
     * EN: Sweeps the translating first OBB with continuous SAT and returns the earliest contact time in [0,1] plus its outward normal.
     */
    public Optional<SweepHit> sweepAgainst(ObbGeometry other, Vec3 movement) {
        if (movement.lengthSqr() <= SWEEP_EPSILON * SWEEP_EPSILON) {
            return Optional.empty();
        }
        AABB movedBounds = worldBounds.move(movement);
        AABB sweptBounds = new AABB(
                Math.min(worldBounds.minX, movedBounds.minX),
                Math.min(worldBounds.minY, movedBounds.minY),
                Math.min(worldBounds.minZ, movedBounds.minZ),
                Math.max(worldBounds.maxX, movedBounds.maxX),
                Math.max(worldBounds.maxY, movedBounds.maxY),
                Math.max(worldBounds.maxZ, movedBounds.maxZ));
        if (!sweptBounds.inflate(SAT_EPSILON).intersects(other.worldBounds)) {
            return Optional.empty();
        }

        ObbProjection first = ObbProjection.of(this);
        ObbProjection second = ObbProjection.of(other);
        Vector3f centerDelta = new Vector3f(first.center()).sub(second.center());
        Vector3f velocity = new Vector3f((float) movement.x(), (float) movement.y(), (float) movement.z());
        SweepTracker tracker = new SweepTracker(centerDelta, velocity);
        for (int i = 0; i < 3; i++) {
            Vector3f axis = first.faceNormal(i);
            if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis))) {
                return Optional.empty();
            }
        }
        for (int i = 0; i < 3; i++) {
            Vector3f axis = second.faceNormal(i);
            if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis))) {
                return Optional.empty();
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector3f axis = crossAxis(first.axis(i), second.axis(j));
                if (axis != null && !tracker.test(axis, projectedRadius(first, axis) + projectedRadius(second, axis))) {
                    return Optional.empty();
                }
            }
        }
        return Optional.ofNullable(tracker.result());
    }

    private static double projectedRadius(ObbProjection projection, Vector3f axis) {
        return projection.extentX * Math.abs(projection.axisX.dot(axis))
                + projection.extentY * Math.abs(projection.axisY.dot(axis))
                + projection.extentZ * Math.abs(projection.axisZ.dot(axis));
    }

    private static Vector3f crossAxis(Vector3f first, Vector3f second) {
        double x = (double) first.y() * second.z() - (double) first.z() * second.y();
        double y = (double) first.z() * second.x() - (double) first.x() * second.z();
        double z = (double) first.x() * second.y() - (double) first.y() * second.x();
        double length = Math.sqrt(x * x + y * y + z * z);
        return length == 0.0 ? null : new Vector3f((float) (x / length), (float) (y / length), (float) (z / length));
    }

    private List<Vec3> vertices(Matrix4f transform) {
        Vec3[] vertices = new Vec3[8];
        Vector3f transformed = new Vector3f();
        int index = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    transform.transformPosition(
                            (float) (x == 0 ? localBounds.minX : localBounds.maxX),
                            (float) (y == 0 ? localBounds.minY : localBounds.maxY),
                            (float) (z == 0 ? localBounds.minZ : localBounds.maxZ),
                            transformed);
                    vertices[index++] = vec3(transformed);
                }
            }
        }
        return List.of(vertices);
    }

    private static Vec3 vec3(Vector3f value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    private double clipLocal(Vector3f from, Vector3f to) {
        double[] range = { 0.0, 1.0 };
        if (!clipAxis(from.x(), to.x() - from.x(), localBounds.minX, localBounds.maxX, range)
                || !clipAxis(from.y(), to.y() - from.y(), localBounds.minY, localBounds.maxY, range)
                || !clipAxis(from.z(), to.z() - from.z(), localBounds.minZ, localBounds.maxZ, range)) {
            return Double.POSITIVE_INFINITY;
        }
        return range[0];
    }

    private static boolean clipAxis(double start, double delta, double min, double max, double[] range) {
        if (Math.abs(delta) < PARALLEL_EPSILON) {
            return start >= min && start <= max;
        }
        double near = (min - start) / delta;
        double far = (max - start) / delta;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }
        range[0] = Math.max(range[0], near);
        range[1] = Math.min(range[1], far);
        return range[0] <= range[1];
    }

    private static boolean clipAxis(double start, double delta, double min, double max, int axis, ClipRange range) {
        if (Math.abs(delta) < PARALLEL_EPSILON) {
            return start >= min && start <= max;
        }

        double minPlane = (min - start) / delta;
        double maxPlane = (max - start) / delta;
        double near;
        double far;
        float entrySign;
        if (minPlane <= maxPlane) {
            near = minPlane;
            far = maxPlane;
            entrySign = -1.0F;
        } else {
            near = maxPlane;
            far = minPlane;
            entrySign = 1.0F;
        }
        if (near > range.near) {
            range.near = near;
            range.entryAxis = axis;
            range.entrySign = entrySign;
        }
        range.far = Math.min(range.far, far);
        return range.near <= range.far;
    }

    /** CN: 将第一个 OBB 推离第二个 OBB 的最小平移。EN: Minimum translation moving the first OBB out of the second. */
    public record Overlap(Vec3 correction, Vec3 normal, double depth) {
    }

    /** CN: OBB 表面命中点和朝外法线。EN: OBB surface hit point and outward normal. */
    public record SurfaceHit(Vec3 position, Vec3 normal) {
    }

    /** CN: 连续 OBB 平移首次接触。EN: First contact of a continuous OBB translation. */
    public record SweepHit(double time, Vec3 normal) {
    }

    private record ObbProjection(Vector3f center, Vector3f axisX, Vector3f axisY, Vector3f axisZ,
            double extentX, double extentY, double extentZ) {
        static ObbProjection of(ObbGeometry geometry) {
            AABB bounds = geometry.localBounds;
            Vector3f center = geometry.localToWorld.transformPosition(
                    (float) ((bounds.minX + bounds.maxX) * 0.5),
                    (float) ((bounds.minY + bounds.maxY) * 0.5),
                    (float) ((bounds.minZ + bounds.maxZ) * 0.5),
                    new Vector3f());
            AxisExtent x = axisExtent(geometry.localToWorld, 1.0F, 0.0F, 0.0F, (bounds.maxX - bounds.minX) * 0.5);
            AxisExtent y = axisExtent(geometry.localToWorld, 0.0F, 1.0F, 0.0F, (bounds.maxY - bounds.minY) * 0.5);
            AxisExtent z = axisExtent(geometry.localToWorld, 0.0F, 0.0F, 1.0F, (bounds.maxZ - bounds.minZ) * 0.5);
            return new ObbProjection(center, x.axis, y.axis, z.axis, x.extent, y.extent, z.extent);
        }

        Vector3f axis(int index) {
            return switch (index) {
                case 0 -> axisX;
                case 1 -> axisY;
                case 2 -> axisZ;
                default -> throw new IllegalArgumentException("Invalid OBB axis index: " + index);
            };
        }

        Vector3f faceNormal(int index) {
            return crossAxis(axis((index + 1) % 3), axis((index + 2) % 3));
        }

        private static AxisExtent axisExtent(Matrix4f matrix, float x, float y, float z, double localExtent) {
            Vector3f axis = matrix.transformDirection(x, y, z, new Vector3f());
            double length = Math.sqrt((double) axis.x() * axis.x()
                    + (double) axis.y() * axis.y() + (double) axis.z() * axis.z());
            if (length == 0.0) {
                return new AxisExtent(new Vector3f(x, y, z), 0.0);
            }
            axis.set((float) (axis.x() / length), (float) (axis.y() / length), (float) (axis.z() / length));
            return new AxisExtent(axis, localExtent * length);
        }
    }

    private record AxisExtent(Vector3f axis, double extent) {
    }

    private static final class ClipRange {
        private double near;
        private double far = 1.0;
        private int entryAxis = -1;
        private float entrySign;
    }

    private static final class OverlapTracker {
        private final Vector3f centerDelta;
        private double minimumDepth = Double.POSITIVE_INFINITY;
        private Vector3f minimumAxis = new Vector3f();

        private OverlapTracker(Vector3f centerDelta) {
            this.centerDelta = centerDelta;
        }

        private boolean test(Vector3f axis, double depth) {
            if (depth < -SAT_EPSILON) {
                return false;
            }
            if (depth < minimumDepth) {
                minimumDepth = Math.max(depth, 0.0);
                minimumAxis = new Vector3f(axis);
                if (centerDelta.dot(minimumAxis) < 0.0F) {
                    minimumAxis.negate();
                }
            }
            return true;
        }

        private Overlap result() {
            if (!Double.isFinite(minimumDepth)) {
                return null;
            }
            double depth = minimumDepth + SAT_EPSILON;
            Vec3 normal = new Vec3(minimumAxis.x(), minimumAxis.y(), minimumAxis.z());
            return new Overlap(normal.scale(depth), normal, depth);
        }
    }

    private static final class SweepTracker {
        private final Vector3f centerDelta;
        private final Vector3f velocity;
        private double entryTime = Double.NEGATIVE_INFINITY;
        private double exitTime = Double.POSITIVE_INFINITY;
        private Vector3f entryAxis;

        private SweepTracker(Vector3f centerDelta, Vector3f velocity) {
            this.centerDelta = centerDelta;
            this.velocity = velocity;
        }

        private boolean test(Vector3f axis, double radius) {
            double distance = centerDelta.dot(axis);
            double speed = velocity.dot(axis);
            if (Math.abs(speed) <= SWEEP_EPSILON) {
                return Math.abs(distance) <= radius + SAT_EPSILON;
            }

            double firstTime = (-radius - distance) / speed;
            double secondTime = (radius - distance) / speed;
            double axisEntry = Math.min(firstTime, secondTime);
            double axisExit = Math.max(firstTime, secondTime);
            if (axisEntry > entryTime) {
                entryTime = axisEntry;
                entryAxis = new Vector3f(axis);
            }
            exitTime = Math.min(exitTime, axisExit);
            return entryTime <= exitTime + SWEEP_EPSILON;
        }

        private SweepHit result() {
            if (entryAxis == null
                    || exitTime < -SWEEP_EPSILON
                    || entryTime < -SAT_EPSILON
                    || entryTime > 1.0 + SWEEP_EPSILON) {
                return null;
            }
            double time = Mth.clamp(entryTime, 0.0, 1.0);
            double distanceAtContact = centerDelta.dot(entryAxis) + velocity.dot(entryAxis) * time;
            if (distanceAtContact < 0.0) {
                entryAxis.negate();
            }
            Vec3 normal = new Vec3(entryAxis.x(), entryAxis.y(), entryAxis.z());
            if (movementInto(normal) >= -SWEEP_EPSILON) {
                return null;
            }
            return new SweepHit(time, normal);
        }

        private double movementInto(Vec3 normal) {
            return velocity.x() * normal.x() + velocity.y() * normal.y() + velocity.z() * normal.z();
        }
    }
}
