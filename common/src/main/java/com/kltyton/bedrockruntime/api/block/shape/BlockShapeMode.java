package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.kltyton.bedrockruntime.api.block.shape.geometry.CompoundShape;

public enum BlockShapeMode {
    AABB,
    OBB;

    public static final com.mojang.serialization.Codec<BlockShapeMode> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(
            name -> switch (name) {
                case "aabb" -> com.mojang.serialization.DataResult.success(AABB);
                case "obb" -> com.mojang.serialization.DataResult.success(OBB);
                default -> com.mojang.serialization.DataResult.error(() -> "Unknown block shape mode: " + name);
            }, mode -> mode.name().toLowerCase(java.util.Locale.ROOT));

    public VoxelShape apply(VoxelShape shape) {
        if (this == OBB || !(shape instanceof CompoundShape)) { return shape; }
        VoxelShape result = Shapes.empty();
        for (var box : shape.toAabbs()) { result = Shapes.or(result, Shapes.create(box)); }
        return result.optimize();
    }
}
