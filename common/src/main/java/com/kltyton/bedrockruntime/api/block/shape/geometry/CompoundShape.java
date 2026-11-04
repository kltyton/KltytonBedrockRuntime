package com.kltyton.bedrockruntime.api.block.shape.geometry;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * VoxelShape entry point with exact convex geometry for model OBBs.
 * The discrete cell and AABBs are broad-phase data; ray, movement, edges and overlap use the actual parts.
 */
public final class CompoundShape extends VoxelShape {
    private final List<ConvexPart> parts;
    private final Vec3 offset;
    private final AABB bounds;
    private final DoubleList xCoords;
    private final DoubleList yCoords;
    private final DoubleList zCoords;

    private CompoundShape(List<ConvexPart> parts, Vec3 offset, AABB bounds) {
        super(BitSetDiscreteVoxelShape.withFilledBounds(1, 1, 1, 0, 0, 0, 1, 1, 1));
        this.parts = parts;
        this.offset = offset;
        this.bounds = bounds;
        xCoords = DoubleList.of(bounds.minX, bounds.maxX);
        yCoords = DoubleList.of(bounds.minY, bounds.maxY);
        zCoords = DoubleList.of(bounds.minZ, bounds.maxZ);
    }

    public static VoxelShape of(List<ConvexPart> parts) {
        if (parts.isEmpty()) { return Shapes.empty(); }
        if (parts.stream().allMatch(ConvexPart::isAxisAligned)) {
            VoxelShape result = Shapes.empty();
            for (ConvexPart part : parts) { result = Shapes.or(result, Shapes.create(part.bounds())); }
            return result.optimize();
        }
        return ofObb(parts);
    }

    /** Keeps axis-aligned parts on the exact OBB collision path when explicitly forced. */
    public static VoxelShape ofObb(List<ConvexPart> parts) {
        if (parts.isEmpty()) { return Shapes.empty(); }
        AABB bounds = parts.get(0).bounds();
        for (int i = 1; i < parts.size(); i++) { bounds = bounds.minmax(parts.get(i).bounds()); }
        return new CompoundShape(List.copyOf(parts), Vec3.ZERO, bounds);
    }

    public List<ConvexPart> partsRelativeTo(Vec3 origin) {
        Vec3 translation = offset.subtract(origin);
        return translation.equals(Vec3.ZERO) ? parts : parts.stream().map(part -> part.move(translation)).toList();
    }

    @Override
    public DoubleList getCoords(Direction.Axis axis) {
        return switch (axis) { case X -> xCoords; case Y -> yCoords; case Z -> zCoords; };
    }

    @Override
    public AABB bounds() { return bounds; }

    @Override
    public VoxelShape optimize() { return this; }

    protected boolean isCubeLike() { return false; }

    @Override
    public VoxelShape move(double x, double y, double z) {
        return x == 0 && y == 0 && z == 0 ? this
                : new CompoundShape(parts, offset.add(x, y, z), bounds.move(x, y, z));
    }

    @Override
    public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
        for (ConvexPart part : parts) {
            for (ConvexPart.Edge edge : part.edges()) {
                Vec3 from = edge.from().add(offset), to = edge.to().add(offset);
                consumer.consume(from.x, from.y, from.z, to.x, to.y, to.z);
            }
        }
    }

    @Override
    public void forAllBoxes(Shapes.DoubleLineConsumer consumer) {
        for (ConvexPart part : parts) {
            AABB box = part.bounds().move(offset);
            consumer.consume(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
    }

    @Override
    public @Nullable BlockHitResult clip(Vec3 from, Vec3 to, BlockPos pos) {
        if (from.distanceToSqr(to) < 1.0E-7) { return null; }
        Vec3 origin = Vec3.atLowerCornerOf(pos).add(offset);
        Vec3 localFrom = from.subtract(origin), localTo = to.subtract(origin);
        ConvexPart.RayHit nearest = null;
        for (ConvexPart part : parts) {
            ConvexPart.RayHit hit = part.clipRay(localFrom, localTo);
            if (hit != null && (nearest == null || hit.time() < nearest.time())) { nearest = hit; }
        }
        if (nearest == null) { return null; }
        Vec3 delta = to.subtract(from);
        Vec3 normal = nearest.normal();
        return new BlockHitResult(from.add(delta.scale(nearest.inside() ? 0.001 : nearest.time())),
                ShapeApiCompat.directionFromNormal(normal), pos, nearest.inside());
    }

    public @Nullable ConvexPart.SweepHit sweep(AABB moving, Vec3 movement) {
        AABB localBox = moving.move(-offset.x, -offset.y, -offset.z);
        ConvexPart.SweepHit earliest = null;
        for (ConvexPart part : parts) {
            ConvexPart.SweepHit hit = part.sweep(localBox, movement);
            if (hit != null && (earliest == null || hit.time() < earliest.time())) { earliest = hit; }
        }
        return earliest;
    }

    public double supportHeight(AABB footprint) {
        double highest = Double.NEGATIVE_INFINITY;
        AABB local = footprint.move(-offset.x, -offset.y, -offset.z);
        for (ConvexPart part : parts) {
            AABB b = part.bounds();
            ConvexPart column = part.intersect(new AABB(local.minX, b.minY - 1, local.minZ,
                    local.maxX, b.maxY + 1, local.maxZ));
            if (column != null) { highest = Math.max(highest, column.bounds().maxY + offset.y); }
        }
        return highest;
    }

    @Override
    public double collide(Direction.Axis axis, AABB moving, double distance) {
        if (Math.abs(distance) < 1.0E-7) { return 0; }
        ConvexPart.SweepHit hit = sweep(moving, Vec3.ZERO.with(axis, distance));
        return hit == null ? distance : distance * hit.time();
    }

    public boolean intersects(AABB box) {
        AABB local = box.move(-offset.x, -offset.y, -offset.z);
        return parts.stream().anyMatch(part -> part.intersects(local));
    }

    public boolean intersects(VoxelShape other) {
        if (other.isEmpty() || !bounds.intersects(other.bounds())) { return false; }
        if (other instanceof CompoundShape compound) {
            List<ConvexPart> otherParts = compound.partsRelativeTo(offset);
            for (ConvexPart first : parts) {
                for (ConvexPart second : otherParts) {
                    if (first.intersects(second)) { return true; }
                }
            }
            return false;
        }
        for (AABB box : other.toAabbs()) {
            if (intersects(box)) { return true; }
        }
        return false;
    }

    @Override
    public VoxelShape getFaceShape(Direction direction) {
        List<ConvexPart> sections = new ArrayList<>();
        for (ConvexPart part : partsRelativeTo(Vec3.ZERO)) {
            ConvexPart section = part.faceSection(direction);
            if (section != null) { sections.add(section); }
        }
        return of(sections);
    }

    @Override
    public double min(Direction.Axis axis, double b, double c) {
        return columnEnd(axis, b, c, false);
    }

    @Override
    public double max(Direction.Axis axis, double b, double c) {
        return columnEnd(axis, b, c, true);
    }

    private double columnEnd(Direction.Axis axis, double b, double c, boolean maximum) {
        Direction.Axis next = Direction.Axis.values()[(axis.ordinal() + 1) % 3];
        Direction.Axis last = Direction.Axis.values()[(axis.ordinal() + 2) % 3];
        Vec3 from = Vec3.ZERO.with(next, b).with(last, c)
                .with(axis, maximum ? bounds.max(axis) + 1 : bounds.min(axis) - 1).subtract(offset);
        Vec3 to = from.with(axis, (maximum ? bounds.min(axis) - 1 : bounds.max(axis) + 1) - offset.get(axis));
        double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (ConvexPart part : parts) {
            ConvexPart.RayHit hit = part.clipRay(from, to);
            if (hit != null) {
                double value = from.add(to.subtract(from).scale(hit.time())).get(axis) + offset.get(axis);
                result = maximum ? Math.max(result, value) : Math.min(result, value);
            }
        }
        return result;
    }

    @Override
    public Optional<Vec3> closestPointTo(Vec3 point) {
        Vec3 local = point.subtract(offset), closest = null;
        for (ConvexPart part : parts) {
            Vec3 candidate = part.closestPoint(local);
            if (closest == null || candidate.distanceToSqr(local) < closest.distanceToSqr(local)) { closest = candidate; }
        }
        return Optional.ofNullable(closest).map(value -> value.add(offset));
    }
}
