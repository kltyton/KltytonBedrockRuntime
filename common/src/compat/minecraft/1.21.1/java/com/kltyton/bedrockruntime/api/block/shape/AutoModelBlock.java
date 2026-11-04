package com.kltyton.bedrockruntime.api.block.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Concrete automatic model block; register one block and its item using the consuming loader's registry. */
public class AutoModelBlock extends AutoPartShapeBlock {
    public static final MapCodec<AutoModelBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(AutoModelBlock::blockId),
            propertiesCodec(),
            BlockShapeMode.CODEC.optionalFieldOf("shape_mode", BlockShapeMode.OBB).forGetter(AutoModelBlock::shapeMode)
    ).apply(instance, AutoModelBlock::new));

    public AutoModelBlock(ResourceLocation blockId, Properties properties, BlockShapeMode mode) {
        super(blockId, properties, mode);
    }

    public AutoModelBlock(ResourceLocation blockId, Properties properties) {
        this(blockId, properties, BlockShapeMode.OBB);
    }

    public final ResourceLocation blockId() {
        return modelId().withPath(path -> path.substring("block/".length()));
    }

    @Override
    public MapCodec<? extends AutoModelBlock> codec() { return CODEC; }
}
