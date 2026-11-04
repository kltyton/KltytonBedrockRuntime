package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.resources.ResourceLocation;

/** Concrete automatic model block; register one block and its item using the consuming loader's registry. */
public class AutoWholeModelBlock extends AutoWholeShapeBlock {
    public AutoWholeModelBlock(ResourceLocation blockId, Properties properties, BlockShapeMode mode) {
        super(blockId, properties, mode);
    }

    public AutoWholeModelBlock(ResourceLocation blockId, Properties properties) {
        this(blockId, properties, BlockShapeMode.OBB);
    }

    public final ResourceLocation blockId() {
        return modelId().withPath(path -> path.substring("block/".length()));
    }

}
