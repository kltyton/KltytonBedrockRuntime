package com.kltyton.bedrockruntime.api.block.shape.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class ShapeApiCompat {
    private ShapeApiCompat() { }
    static VoxelShape move(VoxelShape shape, Vec3 offset) { return shape.move(offset); }
    static Direction directionFromNormal(Vec3 normal) {
        return Direction.getApproximateNearest(normal.x, normal.y, normal.z);
    }
}
