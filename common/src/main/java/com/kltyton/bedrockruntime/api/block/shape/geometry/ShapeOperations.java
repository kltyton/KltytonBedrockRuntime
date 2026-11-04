package com.kltyton.bedrockruntime.api.block.shape.geometry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Exact finite-model boolean operations used where vanilla otherwise reads only the discrete proxy. */
public final class ShapeOperations {
    private ShapeOperations() { }

    public static boolean hasCompound(VoxelShape first, VoxelShape second) {
        return first instanceof CompoundShape || second instanceof CompoundShape;
    }

    public static boolean isFinite(VoxelShape shape) {
        if (shape.isEmpty()) { return true; }
        AABB b = shape.bounds();
        return Double.isFinite(b.minX) && Double.isFinite(b.minY) && Double.isFinite(b.minZ)
                && Double.isFinite(b.maxX) && Double.isFinite(b.maxY) && Double.isFinite(b.maxZ);
    }

    public static VoxelShape union(VoxelShape first, VoxelShape second) {
        return hasCompound(first, second) ? join(first, second, BooleanOp.OR) : Shapes.or(first, second);
    }

    public static boolean joinIsNotEmpty(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) { throw new IllegalArgumentException("Unbounded boolean operation"); }
        if (first.isEmpty() || second.isEmpty()) { return op.apply(!first.isEmpty(), !second.isEmpty()); }
        if (first == second) { return op.apply(true, true); }
        boolean intersects = first instanceof CompoundShape compound ? compound.intersects(second)
                : second instanceof CompoundShape compound ? compound.intersects(first)
                : Shapes.joinIsNotEmpty(first, second, BooleanOp.AND);
        return op.apply(true, true) && intersects
                || op.apply(true, false) && hasDifference(first, second)
                || op.apply(false, true) && hasDifference(second, first);
    }

    private static boolean hasDifference(VoxelShape first, VoxelShape second) {
        if (first.isEmpty()) { return false; }
        if (second.isEmpty()) { return true; }
        if (!isFinite(first) && isFinite(second)) { return true; }
        Vec3 origin = center(first.bounds());
        AABB region = first.bounds().move(-origin.x, -origin.y, -origin.z);
        List<ConvexPart> remaining = partsWithin(first, origin, region);
        for (ConvexPart subtractor : partsWithin(second, origin, region)) {
            remaining = subtract(remaining, subtractor);
            if (remaining.isEmpty()) { return false; }
        }
        return !remaining.isEmpty();
    }

    public static VoxelShape join(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) { throw new IllegalArgumentException("Unbounded boolean operation"); }
        if (first.isEmpty()) { return op.apply(false, true) ? second : Shapes.empty(); }
        if (second.isEmpty()) { return op.apply(true, false) ? first : Shapes.empty(); }
        if (first == second) { return op.apply(true, true) ? first : Shapes.empty(); }
        boolean both = op.apply(true, true), onlyFirst = op.apply(true, false), onlySecond = op.apply(false, true);
        if (both && onlyFirst && !onlySecond) { return first; }
        if (both && onlySecond && !onlyFirst) { return second; }
        if (!both && !onlyFirst && !onlySecond) { return Shapes.empty(); }
        Vec3 origin = center(first.bounds());
        AABB region = first.bounds().minmax(second.bounds()).move(-origin.x, -origin.y, -origin.z);
        List<ConvexPart> a = partsWithin(first, origin, region), b = partsWithin(second, origin, region);
        List<ConvexPart> result = new ArrayList<>();
        if (both && onlyFirst && onlySecond) {
            result.addAll(a); result.addAll(b);
        } else if (both) {
            for (ConvexPart x : a) {
                for (ConvexPart y : b) {
                    ConvexPart intersection = x.intersect(y);
                    if (intersection != null) { result.add(intersection); }
                }
            }
        } else {
            if (onlyFirst) { result.addAll(difference(a, b)); }
            if (onlySecond) { result.addAll(difference(b, a)); }
        }
        return ShapeApiCompat.move(CompoundShape.of(result), origin);
    }

    private static List<ConvexPart> difference(List<ConvexPart> first, List<ConvexPart> second) {
        List<ConvexPart> result = first;
        for (ConvexPart subtractor : second) {
            result = subtract(result, subtractor);
            if (result.isEmpty()) { break; }
        }
        return result;
    }

    private static List<ConvexPart> subtract(List<ConvexPart> parts, ConvexPart subtractor) {
        List<ConvexPart> result = new ArrayList<>();
        for (ConvexPart part : parts) { result.addAll(part.subtract(subtractor)); }
        return result;
    }

    private static List<ConvexPart> partsWithin(VoxelShape shape, Vec3 origin, AABB region) {
        if (shape instanceof CompoundShape compound) { return compound.partsRelativeTo(origin); }
        List<ConvexPart> parts = new ArrayList<>();
        for (AABB raw : shape.toAabbs()) {
            AABB box = raw.move(-origin.x, -origin.y, -origin.z);
            if (box.intersects(region)) {
                parts.add(ConvexPart.box(new AABB(Math.max(box.minX, region.minX), Math.max(box.minY, region.minY),
                        Math.max(box.minZ, region.minZ), Math.min(box.maxX, region.maxX), Math.min(box.maxY, region.maxY),
                        Math.min(box.maxZ, region.maxZ))));
            }
        }
        return parts;
    }

    private static Vec3 center(AABB b) {
        return new Vec3((b.minX + b.maxX) * 0.5, (b.minY + b.maxY) * 0.5, (b.minZ + b.maxZ) * 0.5);
    }
}
