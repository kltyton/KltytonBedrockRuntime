package com.kltyton.bedrockruntime.client.particle.runtime.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Sweeps particle centers against terrain, then resolves normal and tangential velocity. */
public final class ParticleCollision {
    private static final double DIRECTION_EPSILON = 1.1920928955078125E-7;
    private static final double CONTACT_BIAS = 3.0517578125E-5;

    private ParticleCollision() {}

    public static Result resolve(Level level, Vec3 previous, Vec3 position, Vec3 velocity,
                                 float radius, float drag, float restitution) {
        Vec3 displacement = position.subtract(previous);
        double length = displacement.length();
        if (length > 1.0) previous = position.subtract(displacement.scale(1.0 / length));
        AABB query = new AABB(Math.min(previous.x, position.x) - radius, Math.min(previous.y, position.y) - radius,
                Math.min(previous.z, position.z) - radius, Math.max(previous.x, position.x) + radius,
                Math.max(previous.y, position.y) + radius, Math.max(previous.z, position.z) + radius);
        List<AABB> shapes = new ArrayList<>();
        BlockPos.MutableBlockPos block = new BlockPos.MutableBlockPos();
        for (int bx = Mth.floor(query.minX) - 1; bx <= Mth.floor(query.maxX) + 1; bx++) {
            for (int bz = Mth.floor(query.minZ) - 1; bz <= Mth.floor(query.maxZ) + 1; bz++) {
                for (int by = Mth.floor(query.maxY) + 1; by >= Mth.floor(query.minY) - 1; by--) {
                    block.set(bx, by, bz);
                    VoxelShape shape = level.getBlockState(block).getCollisionShape(level, block, CollisionContext.empty());
                    if (shape.isEmpty()) continue;
                    for (AABB box : shape.toAabbs()) {
                        AABB worldBox = box.move(bx, by, bz);
                        if (worldBox.intersects(query)) shapes.add(worldBox);
                    }
                }
            }
        }
        Vec3 normal = Vec3.ZERO;
        boolean collided = false;
        for (int pass = 0; pass < 3; pass++) {
            Hit closest = null;
            for (AABB shape : shapes) {
                Hit hit = sweep(previous, position, shape.inflate(radius));
                if (hit != null && (closest == null || hit.fraction() < closest.fraction())) closest = hit;
            }
            if (closest == null) break;
            collided = true;
            normal = closest.normal();
            double fraction = closest.fraction();
            if (fraction != 1.0 && fraction > CONTACT_BIAS) fraction -= CONTACT_BIAS;
            double remaining = position.subtract(previous).dot(normal) * (1.0 - fraction);
            position = position.subtract(normal.scale(remaining));
            velocity = velocity.subtract(normal.scale((1.0 + restitution) * velocity.dot(normal)));
            Vec3 tangent = velocity.subtract(normal.scale(velocity.dot(normal)));
            double tangentLength = tangent.length();
            if (tangentLength > 0.0) {
                double tangentScale = tangentLength < 1.0E-4 ? 0.0 : Math.max(tangentLength - drag, 0.0) / tangentLength;
                velocity = velocity.add(tangent.scale(tangentScale - 1.0));
            }
        }
        return new Result(position, velocity, normal, collided);
    }

    private static @Nullable Hit sweep(Vec3 start, Vec3 end, AABB box) {
        double enter = -Double.MAX_VALUE;
        double exit = Double.MAX_VALUE;
        int contactAxis = 0;
        for (int axis = 0; axis < 3; axis++) {
            double from = axis == 0 ? start.x : axis == 1 ? start.y : start.z;
            double to = axis == 0 ? end.x : axis == 1 ? end.y : end.z;
            double minimum = axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
            double maximum = axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
            double direction = to - from;
            if (Math.abs(direction) < DIRECTION_EPSILON) {
                if (to <= minimum || to >= maximum) return null;
                continue;
            }
            double near = (minimum - from) / direction;
            double far = (maximum - from) / direction;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            if (near >= enter) {
                enter = near;
                contactAxis = axis;
            }
            exit = Math.min(exit, far);
        }
        if (enter < 0.0 || enter > 1.0 || enter > exit) return null;
        Vec3 normal = contactAxis == 0 ? new Vec3(1.0, 0.0, 0.0)
                : contactAxis == 1 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(0.0, 0.0, 1.0);
        return new Hit(enter, normal);
    }

    private record Hit(double fraction, Vec3 normal) {}

    public record Result(Vec3 position, Vec3 velocity, Vec3 normal, boolean collided) {}
}
