package com.kltyton.bedrockruntime.api.block.shape.geometry;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BlockShapeCandidatesCompat {
    private BlockShapeCandidatesCompat() { }
    static void append(Level level, Entity entity, CollisionContext context, AABB expanded, List<VoxelShape> result) {
        level.getBlockCollisionsFromContext(context, expanded).forEach(result::add);
    }
}
