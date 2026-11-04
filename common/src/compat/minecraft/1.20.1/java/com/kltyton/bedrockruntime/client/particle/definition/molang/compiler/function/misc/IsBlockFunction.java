package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.StringValue;

import java.util.function.Predicate;

public final class IsBlockFunction extends MathFunction {
    private final MathValue argument;
    private final Predicate<BlockState> constantMatcher;

    public IsBlockFunction(MathValue... values) {
        super(values);
        this.argument = values[0];
        this.constantMatcher = argument instanceof StringValue literal ? matcher(literal.value()) : null;
    }

    @Override
    public String getName() {
        return "query.is_block";
    }

    @Override
    public double compute(MolangInstance instance) {
        BlockState state = instance.getLevel().getBlockState(BlockPos.containing(instance.getPosition()));
        String value = constantMatcher == null ? argument.getString(instance) : null;
        if (constantMatcher == null && value == null) {
            throw new IllegalArgumentException("query.is_block requires a string argument");
        }
        Predicate<BlockState> matcher = constantMatcher == null ? matcher(value) : constantMatcher;
        return matcher.test(state) ? 1.0 : 0.0;
    }

    private static Predicate<BlockState> matcher(String value) {
        if (value.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, new ResourceLocation(value.substring(1)));
            return state -> state.is(tag);
        }
        return BuiltInRegistries.BLOCK.getHolder(ResourceKey.create(Registries.BLOCK, new ResourceLocation(value)))
                .<Predicate<BlockState>>map(holder -> state -> state.is(holder.value()))
                .orElse(state -> false);
    }

    @Override
    public boolean isMutable(MathValue... values) {
        return true;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{argument};
    }
}
