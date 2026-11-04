package com.kltyton.bedrockruntime.api.block.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Concrete automatic model block; register one block and its item using the consuming loader's registry. */
public class AutoWholeModelBlock extends AutoWholeShapeBlock {
    public static final MapCodec<AutoWholeModelBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("block").forGetter(AutoWholeModelBlock::blockId),
            propertiesCodec(),
            BlockShapeMode.CODEC.optionalFieldOf("shape_mode", BlockShapeMode.OBB).forGetter(AutoWholeModelBlock::shapeMode)
    ).apply(instance, AutoWholeModelBlock::new));

    public AutoWholeModelBlock(Identifier blockId, Properties properties, BlockShapeMode mode) {
        super(blockId, properties, mode);
    }

    public AutoWholeModelBlock(Identifier blockId, Properties properties) {
        this(blockId, properties, BlockShapeMode.OBB);
    }

    public final Identifier blockId() {
        return modelId().withPath(path -> path.substring("block/".length()));
    }

    @Override
    public MapCodec<? extends AutoWholeModelBlock> codec() { return CODEC; }
}
