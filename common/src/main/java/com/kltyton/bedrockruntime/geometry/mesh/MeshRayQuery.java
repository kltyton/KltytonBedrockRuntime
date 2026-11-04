package com.kltyton.bedrockruntime.geometry.mesh;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Converts a world segment to an exact mesh query without losing nonuniform or negative scale. */
public final class MeshRayQuery {
    private MeshRayQuery() { }

    public static double clipFraction(TriangleMesh mesh, AABB region, Matrix4fc worldToLocal, Vec3 from, Vec3 to) {
        Vector3f start = worldToLocal.transformPosition((float) from.x(), (float) from.y(), (float) from.z(), new Vector3f());
        Vector3f end = worldToLocal.transformPosition((float) to.x(), (float) to.y(), (float) to.z(), new Vector3f());
        double dx = (double) end.x - start.x;
        double dy = (double) end.y - start.y;
        double dz = (double) end.z - start.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double fraction = mesh.rayIntersection(start.x, start.y, start.z,
                dx / length, dy / length, dz / length, length, true) / length;
        if (!Double.isFinite(fraction) || !region.inflate(1.0E-7).contains(
                start.x + dx * fraction, start.y + dy * fraction, start.z + dz * fraction)) {
            return Double.POSITIVE_INFINITY;
        }
        return fraction;
    }
}
