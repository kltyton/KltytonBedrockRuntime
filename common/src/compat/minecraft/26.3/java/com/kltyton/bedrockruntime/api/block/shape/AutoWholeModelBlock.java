package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.resources.Identifier;

/** Concrete automatic model block; register one block and its item using the consuming loader's registry. */
public class AutoWholeModelBlock extends AutoWholeShapeBlock {
    public AutoWholeModelBlock(Identifier blockId, Properties properties, BlockShapeMode mode) {
        super(blockId, properties, mode);
    }

    public AutoWholeModelBlock(Identifier blockId, Properties properties) {
        this(blockId, properties, BlockShapeMode.OBB);
    }

    public final Identifier blockId() {
        return modelId().withPath(path -> path.substring("block/".length()));
    }
}
