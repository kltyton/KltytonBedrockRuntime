package com.kltyton.bedrockruntime.api.block.shape.geometry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Continuous block collision with native vertical support and sliding along authored slanted faces. */
public final class BlockShapeCollision {
    private static final double EPSILON = 1.0E-9;
    private static final int MAX_CONTACTS = 8;

    private BlockShapeCollision() { }

    public static boolean containsCompound(List<VoxelShape> shapes) {
        return shapes.stream().anyMatch(CompoundShape.class::isInstance);
    }

    public static Vec3 collide(Vec3 requested, AABB box, List<VoxelShape> shapes) {
        // Gravity stays vertical: a resting entity must not acquire sideways velocity on a slanted top.
        double y = Shapes.collide(Direction.Axis.Y, box, shapes, requested.y);
        AABB supported = box.move(0, y, 0);
        Vec3 remaining = new Vec3(requested.x, 0, requested.z);
        Vec3 resolved = Vec3.ZERO;
        for (int contact = 0; contact < MAX_CONTACTS && remaining.lengthSqr() > EPSILON * EPSILON; contact++) {
            ConvexPart.SweepHit hit = firstHit(supported.move(resolved), remaining, shapes);
            if (hit == null) {
                resolved = resolved.add(remaining);
                break;
            }
            resolved = resolved.add(remaining.scale(hit.time()));
            remaining = remaining.scale(1 - hit.time());
            double intoSurface = remaining.dot(hit.normal());
            if (intoSurface < 0) { remaining = remaining.subtract(hit.normal().scale(intoSurface)); }
        }
        return new Vec3(resolved.x, y + resolved.y, resolved.z);
    }

    private static @Nullable ConvexPart.SweepHit firstHit(AABB box, Vec3 movement, List<VoxelShape> shapes) {
        ConvexPart.SweepHit earliest = null;
        for (VoxelShape shape : shapes) {
            if (shape instanceof CompoundShape compound) {
                ConvexPart.SweepHit hit = compound.sweep(box, movement);
                if (hit != null && (earliest == null || hit.time() < earliest.time())) { earliest = hit; }
            } else {
                for (AABB obstacle : shape.toAabbs()) {
                    ConvexPart.SweepHit hit = sweepBox(box, obstacle, movement);
                    if (hit != null && (earliest == null || hit.time() < earliest.time())) { earliest = hit; }
                }
            }
        }
        return earliest;
    }

    private static @Nullable ConvexPart.SweepHit sweepBox(AABB moving, AABB obstacle, Vec3 movement) {
        double entry = Double.NEGATIVE_INFINITY, exit = Double.POSITIVE_INFINITY;
        Vec3 normal = Vec3.ZERO;
        for (Direction.Axis axis : Direction.Axis.values()) {
            double speed = movement.get(axis);
            if (Math.abs(speed) <= EPSILON) {
                if (moving.max(axis) <= obstacle.min(axis) + EPSILON || moving.min(axis) >= obstacle.max(axis) - EPSILON) { return null; }
                continue;
            }
            double near = (obstacle.min(axis) - moving.max(axis)) / speed;
            double far = (obstacle.max(axis) - moving.min(axis)) / speed;
            if (near > far) { double swap = near; near = far; far = swap; }
            if (near > entry) {
                entry = near;
                normal = Vec3.ZERO.with(axis, speed > 0 ? -1 : 1);
            }
            exit = Math.min(exit, far);
            if (entry > exit + EPSILON) { return null; }
        }
        return entry >= -EPSILON && entry <= 1 && exit >= 0 && movement.dot(normal) < -EPSILON
                ? new ConvexPart.SweepHit(Math.max(0, entry), normal) : null;
    }

    /**
     * A collision can deflect movement outside its original axis-aligned sweep.
     * Re-query only when an OBB participates, covering every direction within the sweep's diagonal.
     */
    public static List<VoxelShape> expandedCandidates(@Nullable Entity entity, CollisionContext context, Level level,
            List<VoxelShape> entityColliders, AABB query, List<VoxelShape> initial, boolean includeBorder) {
        if (!containsCompound(initial)) { return initial; }
        double radius = Math.sqrt(query.getXsize() * query.getXsize() + query.getYsize() * query.getYsize()
                + query.getZsize() * query.getZsize());
        AABB expanded = query.inflate(radius);
        List<VoxelShape> result = new ArrayList<>(entityColliders);
        if (entity != null) { result.addAll(level.getEntityCollisions(entity, expanded)); }
        if (includeBorder && entity != null && level.getWorldBorder().isInsideCloseToBorder(entity, expanded)) {
            result.add(level.getWorldBorder().getCollisionShape());
        }
        BlockShapeCandidatesCompat.append(level, entity, context, expanded, result);
        return result;
    }
}
