package com.kltyton.bedrockruntime.client.particle.runtime.physics;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/** Sweeps particle centers against terrain, then resolves normal and tangential velocity. */
public final class ParticleCollision {
    private static final double DIRECTION_EPSILON = 1.1920928955078125E-7;
    private static final double CONTACT_BIAS = 3.0517578125E-5;

    private ParticleCollision() {}

    public static Result resolve(ClientLevel level, Vec3 previous, Vec3 position, Vec3 velocity,
                                 float radius, float drag, float restitution, boolean stopOnContact) {
        double motionSquared = position.distanceToSqr(previous);
        if (!Double.isFinite(motionSquared) || motionSquared < DIRECTION_EPSILON * DIRECTION_EPSILON) {
            return new Result(position, velocity, null, false);
        }
        // Allow for boxes protruding past their source block, such as fences and moving pistons.
        double margin = radius + 1.0 + CONTACT_BIAS;
        AABB bounds = terrainBounds(level).inflate(margin);
        Contact first = null;
        boolean grounded = false;
        for (int pass = 0; pass < 3; pass++) {
            Hit closest = firstHit(level, bounds, previous, position, radius, margin);
            if (closest == null) return new Result(position, velocity, first, grounded);
            Vec3 normal = closest.normal();
            Vec3 contactPoint = previous.lerp(position, closest.fraction());
            if (first == null) first = new Contact(contactPoint, normal, velocity);
            grounded |= normal.y > 0.0;
            if (stopOnContact) return new Result(contactPoint, velocity, first, grounded);

            Vec3 remainder = position.subtract(contactPoint);
            previous = contactPoint.add(normal.scale(CONTACT_BIAS));
            position = previous.add(remainder.subtract(normal.scale(remainder.dot(normal))));
            double approach = velocity.dot(normal);
            if (approach < 0.0) velocity = velocity.subtract(normal.scale((1.0 + restitution) * approach));
            Vec3 tangent = velocity.subtract(normal.scale(velocity.dot(normal)));
            double tangentLength = tangent.length();
            if (tangentLength > 0.0) {
                double tangentScale = tangentLength < 1.0E-4 ? 0.0 : Math.max(tangentLength - drag, 0.0) / tangentLength;
                velocity = velocity.add(tangent.scale(tangentScale - 1.0));
            }
        }
        return new Result(previous, velocity, first, grounded);
    }

    private static AABB terrainBounds(ClientLevel level) {
        var storage = level.getChunkSource().storage;
        int radius = storage.chunkRadius;
        int x = storage.viewCenterX;
        int z = storage.viewCenterZ;
        return new AABB(Math.max(-Level.MAX_LEVEL_SIZE, (x - radius) * 16.0), level.getMinY(),
                Math.max(-Level.MAX_LEVEL_SIZE, (z - radius) * 16.0),
                Math.min(Level.MAX_LEVEL_SIZE, (x + radius + 1) * 16.0), level.getMaxY() + 1.0,
                Math.min(Level.MAX_LEVEL_SIZE, (z + radius + 1) * 16.0));
    }

    private static @Nullable Hit firstHit(ClientLevel level, AABB bounds, Vec3 from, Vec3 to, float radius, double margin) {
        if (from.equals(to)) return null;
        Vec3 scanFrom = bounds.contains(from) ? from : bounds.clip(from, to).orElse(null);
        if (scanFrom == null) return null;
        Vec3 scanTo = bounds.contains(to) ? to : bounds.clip(to, from).orElse(null);
        if (scanTo == null) return null;
        var sweep = new TerrainSweep(level, from, to, radius);
        var box = AABB.ofSize(scanTo, 2.0 * margin, 2.0 * margin, 2.0 * margin);
        BlockGetter.forEachBlockIntersectedBetween(scanFrom, scanTo, box, sweep::visit);
        return sweep.closest;
    }

    private static final class TerrainSweep {
        private final ClientLevel level;
        private final Vec3 from;
        private final Vec3 to;
        private final float radius;
        private @Nullable Hit closest;

        private TerrainSweep(ClientLevel level, Vec3 from, Vec3 to, float radius) {
            this.level = level;
            this.from = from;
            this.to = to;
            this.radius = radius;
        }

        private boolean visit(BlockPos pos, int step) {
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) return true;
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.empty());
            if (shape.isEmpty()) return true;
            for (AABB box : shape.toAabbs()) {
                Hit hit = sweep(from, to, box.move(pos.getX(), pos.getY(), pos.getZ()).inflate(radius));
                if (hit != null && (closest == null || hit.fraction() < closest.fraction())) closest = hit;
            }
            return true;
        }
    }

    private static @Nullable Hit sweep(Vec3 start, Vec3 end, AABB box) {
        double enter = -Double.MAX_VALUE;
        double exit = Double.MAX_VALUE;
        int contactAxis = 0;
        double contactSign = 1.0;
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
                contactSign = -Math.signum(direction);
            }
            exit = Math.min(exit, far);
        }
        if (enter < 0.0 || enter > 1.0 || enter > exit) return null;
        Vec3 normal = contactAxis == 0 ? new Vec3(contactSign, 0.0, 0.0)
                : contactAxis == 1 ? new Vec3(0.0, contactSign, 0.0) : new Vec3(0.0, 0.0, contactSign);
        return new Hit(enter, normal);
    }

    private record Hit(double fraction, Vec3 normal) {}

    public record Contact(Vec3 point, Vec3 normal, Vec3 velocity) {}

    public record Result(Vec3 position, Vec3 velocity, @Nullable Contact contact, boolean grounded) {}
}
