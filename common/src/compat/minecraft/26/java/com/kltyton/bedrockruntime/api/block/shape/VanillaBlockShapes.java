package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Immutable packaged model geometry shared by both logical sides; queries never load resources. */
public final class VanillaBlockShapes {
    private VanillaBlockShapes() { }

    public static void initialize() {
        VanillaBlockShapesCore.initialize();
    }

    /** Called after every mod has completed block registration, before either side begins ticking. */
    public static synchronized void initializeModBlocks() {
        VanillaBlockShapesCore.initializeModBlocks();
    }

    public static boolean contains(BlockState state) {
        return VanillaBlockShapesCore.contains(state);
    }

    public static VoxelShape replacement(BlockState state, BlockGetter level, BlockPos pos, VoxelShape original) {
        return VanillaBlockShapesCore.replacement(state, level, pos, original);
    }

    public static @Nullable VoxelShape shape(BlockState state, BlockPos pos) {
        return VanillaBlockShapesCore.shape(state, pos);
    }
}
