package com.kltyton.bedrockruntime.api.block.shape.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class ShapeApiCompat {
    private ShapeApiCompat() { }
    static VoxelShape move(VoxelShape shape, Vec3 offset) { return shape.move(offset.x, offset.y, offset.z); }
    static Direction directionFromNormal(Vec3 normal) {
        return Direction.getNearest((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
