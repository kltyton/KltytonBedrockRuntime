package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Automatic model-derived multi-cell block whose visual selection outline is
 * the complete assembled model regardless of which linked cell is targeted.
 *
 * <p>Physical collision and ray targeting remain local to each occupied cell;
 * only the rendered selection outline differs from {@link AutoPartShapeBlock}.</p>
 */
public abstract class AutoWholeShapeBlock extends AutoPartShapeBlock {
    protected AutoWholeShapeBlock(Identifier blockId, Properties properties) {
        super(blockId, properties);
    }

    protected AutoWholeShapeBlock(Identifier blockId, Properties properties, BlockShapeMode mode) {
        super(blockId, properties, mode);
    }

    /**
     * Returns the complete outline local to {@link #getOriginPosition}.
     */
    public final VoxelShape getWholeOutlineShape(
            BlockGetter level,
            BlockState state,
            BlockPos partPos) {
        return wholeShape(level, state, partPos);
    }
}
