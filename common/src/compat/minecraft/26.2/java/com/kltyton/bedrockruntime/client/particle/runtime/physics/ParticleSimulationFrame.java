package com.kltyton.bedrockruntime.client.particle.runtime.physics;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.jetbrains.annotations.Nullable;

/** Converts between particle simulation coordinates and the world frame used by terrain. */
public record ParticleSimulationFrame(Matrix3fc toWorld, Vec3 translation) {
    public Vec3 point(double x, double y, double z) {
        return apply(toWorld, x, y, z).add(translation);
    }

    public Vec3 direction(Vec3 vector) {
        return apply(toWorld, vector.x, vector.y, vector.z);
    }

    public @Nullable Inverse inverse() {
        float determinant = toWorld.determinant();
        if (!Float.isFinite(determinant) || determinant == 0) return null;
        var inverse = toWorld.invert(new Matrix3f());
        return inverse.isFinite() ? new Inverse(inverse, translation) : null;
    }

    public record Inverse(Matrix3fc toSimulation, Vec3 worldOrigin) {
        public Vec3 point(Vec3 world) { return direction(world.subtract(worldOrigin)); }

        public Vec3 direction(Vec3 world) { return apply(toSimulation, world.x, world.y, world.z); }
    }

    private static Vec3 apply(Matrix3fc matrix, double x, double y, double z) {
        return new Vec3(matrix.m00() * x + matrix.m10() * y + matrix.m20() * z,
                matrix.m01() * x + matrix.m11() * y + matrix.m21() * z,
                matrix.m02() * x + matrix.m12() * y + matrix.m22() * z);
    }
}
