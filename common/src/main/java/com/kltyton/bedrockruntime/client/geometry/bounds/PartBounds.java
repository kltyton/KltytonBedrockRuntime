package com.kltyton.bedrockruntime.client.geometry.bounds;

import com.kltyton.bedrockruntime.geometry.obb.ObbGeometry;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.MeshRayQuery;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * CN: 单个视觉 cube 的本地空间、渲染空间和 OBB 变换。
 * EN: Local-space, render-space, and OBB transforms for one visual cube.
 */
public record PartBounds(AABB renderBounds, AABB localBounds, VoxelShape localShape, Matrix4f localToRender,
                         Matrix4f renderToLocal, TriangleMesh mesh) {
    private static final double MIN_SIZE = 1.0E-4;
    private static final double PARALLEL_EPSILON = 1.0E-7;

    public PartBounds(AABB renderBounds, AABB localBounds, VoxelShape localShape,
                      Matrix4f localToRender, Matrix4f renderToLocal) {
        this(renderBounds, localBounds, localShape, localToRender, renderToLocal, null);
    }

    /** Mesh rays use the authored surface; movement and contacts retain the fitted OBB. */
    public static PartBounds fromMesh(Matrix4f pose, TriangleMesh mesh, AABB fittedBounds) {
        Matrix4f localToRender = VanillaCubeCapture.pose(pose);
        if (!localToRender.isFinite() || Math.abs(localToRender.determinant()) < PARALLEL_EPSILON) {
            return null;
        }
        AABB renderBounds = ObbGeometry.fromLocalBounds(fittedBounds, localToRender).worldBounds();
        VoxelShape localShape = Shapes.box(fittedBounds.minX, fittedBounds.minY, fittedBounds.minZ,
                fittedBounds.maxX, fittedBounds.maxY, fittedBounds.maxZ);
        return new PartBounds(renderBounds, fittedBounds, localShape, localToRender,
                new Matrix4f(localToRender).invert(), mesh);
    }

    public static PartBounds fromVanillaCube(Matrix4f pose, ModelPart.Cube cube) {
        BoundsBuilder builder = builder(pose);
        VanillaCubeCapture.include(cube, builder);
        return builder.build();
    }

    public static BoundsBuilder builder(Matrix4f pose) {
        return new BoundsBuilder(pose);
    }

    public PartBounds translateRenderSpace(Vec3 translation) {
        return new PartBounds(renderBounds.move(translation), localBounds, localShape,
                new Matrix4f().translation((float) translation.x, (float) translation.y, (float) translation.z)
                        .mul(localToRender),
                new Matrix4f(renderToLocal).translate((float) -translation.x, (float) -translation.y, (float) -translation.z), mesh);
    }

    public Matrix4f localToWorld(Vec3 cameraPosition) {
        return new Matrix4f()
                .translation((float) cameraPosition.x(), (float) cameraPosition.y(), (float) cameraPosition.z())
                .mul(localToRender);
    }

    public Matrix4f worldToLocal(Vec3 cameraPosition) {
        return new Matrix4f(renderToLocal)
                .translate((float) -cameraPosition.x(), (float) -cameraPosition.y(), (float) -cameraPosition.z());
    }

    public AABB worldBounds(Vec3 cameraPosition) {
        return renderBounds.move(cameraPosition);
    }

    public ObbGeometry geometry(Vec3 cameraPosition) {
        return new ObbGeometry(localBounds, worldBounds(cameraPosition), localToWorld(cameraPosition), worldToLocal(cameraPosition), mesh);
    }

    public double clipDistanceSqr(Vec3 from, Vec3 to) {
        if (!renderBounds.contains(from) && renderBounds.clip(from, to).isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        double hitT;
        if (mesh == null) {
            Vector3f localFrom = renderToLocal.transformPosition((float) from.x(), (float) from.y(), (float) from.z(), new Vector3f());
            Vector3f localTo = renderToLocal.transformPosition((float) to.x(), (float) to.y(), (float) to.z(), new Vector3f());
            hitT = clipLocal(localFrom, localTo);
        } else {
            hitT = MeshRayQuery.clipFraction(mesh, localBounds, renderToLocal, from, to);
        }
        if (!Double.isFinite(hitT)) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        return (dx * dx + dy * dy + dz * dz) * hitT * hitT;
    }

    private double clipLocal(Vector3f from, Vector3f to) {
        double[] range = { 0.0, 1.0 };
        if (!clipAxis(from.x(), to.x() - from.x(), localBounds.minX, localBounds.maxX, range)) {
            return Double.POSITIVE_INFINITY;
        }
        if (!clipAxis(from.y(), to.y() - from.y(), localBounds.minY, localBounds.maxY, range)) {
            return Double.POSITIVE_INFINITY;
        }
        if (!clipAxis(from.z(), to.z() - from.z(), localBounds.minZ, localBounds.maxZ, range)) {
            return Double.POSITIVE_INFINITY;
        }
        return range[0];
    }

    private static boolean clipAxis(double start, double delta, double min, double max, double[] range) {
        if (Math.abs(delta) < PARALLEL_EPSILON) {
            return start >= min && start <= max;
        }

        double invDelta = 1.0 / delta;
        double near = (min - start) * invDelta;
        double far = (max - start) * invDelta;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }

        range[0] = Math.max(range[0], near);
        range[1] = Math.min(range[1], far);
        return range[0] <= range[1];
    }

    private static double expandMin(double min, double max) {
        return max - min < MIN_SIZE ? min - MIN_SIZE : min;
    }

    private static double expandMax(double min, double max) {
        return max - min < MIN_SIZE ? max + MIN_SIZE : max;
    }

    public static final class BoundsBuilder {
        private final Matrix4f pose;
        private double localMinX = Double.POSITIVE_INFINITY;
        private double localMinY = Double.POSITIVE_INFINITY;
        private double localMinZ = Double.POSITIVE_INFINITY;
        private double localMaxX = Double.NEGATIVE_INFINITY;
        private double localMaxY = Double.NEGATIVE_INFINITY;
        private double localMaxZ = Double.NEGATIVE_INFINITY;

        private BoundsBuilder(Matrix4f pose) {
            this.pose = VanillaCubeCapture.pose(pose);
        }

        public void include(float x, float y, float z) {
            localMinX = Math.min(localMinX, x);
            localMinY = Math.min(localMinY, y);
            localMinZ = Math.min(localMinZ, z);
            localMaxX = Math.max(localMaxX, x);
            localMaxY = Math.max(localMaxY, y);
            localMaxZ = Math.max(localMaxZ, z);

        }

        public PartBounds build() {
            if (!Double.isFinite(localMinX) || !Double.isFinite(localMinY) || !Double.isFinite(localMinZ)
                    || !Double.isFinite(localMaxX) || !Double.isFinite(localMaxY) || !Double.isFinite(localMaxZ)
                    || !pose.isFinite()) {
                return null;
            }
            if (Math.abs(pose.determinant()) < PARALLEL_EPSILON) {
                return null;
            }

            double safeLocalMinX = expandMin(localMinX, localMaxX);
            double safeLocalMinY = expandMin(localMinY, localMaxY);
            double safeLocalMinZ = expandMin(localMinZ, localMaxZ);
            double safeLocalMaxX = expandMax(localMinX, localMaxX);
            double safeLocalMaxY = expandMax(localMinY, localMaxY);
            double safeLocalMaxZ = expandMax(localMinZ, localMaxZ);

            AABB localBounds = new AABB(safeLocalMinX, safeLocalMinY, safeLocalMinZ, safeLocalMaxX, safeLocalMaxY, safeLocalMaxZ);
            AABB renderBounds = ObbGeometry.fromLocalBounds(localBounds, pose).worldBounds();
            VoxelShape localShape = Shapes.box(safeLocalMinX, safeLocalMinY, safeLocalMinZ, safeLocalMaxX, safeLocalMaxY, safeLocalMaxZ);
            Matrix4f localToRender = new Matrix4f(pose);
            Matrix4f renderToLocal = new Matrix4f(pose).invert();
            return new PartBounds(renderBounds, localBounds, localShape, localToRender, renderToLocal);
        }
    }
}
