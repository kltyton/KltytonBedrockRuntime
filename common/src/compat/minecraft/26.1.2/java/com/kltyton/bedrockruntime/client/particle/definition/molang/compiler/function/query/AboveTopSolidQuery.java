package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;

public final class AboveTopSolidQuery extends MathFunction {
    private final MathValue[] arguments;

    public AboveTopSolidQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!ActorAccess.hasBlockSource(instance) || arguments.length != 2) return 0.0;
        Level level = instance.getLevel();
        int x = roundedCoordinate(arguments[0].get(instance));
        int z = roundedCoordinate(arguments[1].get(instance));
        if (!level.hasChunk(x >> 4, z >> 4)) return level.getMinY();
        var chunk = level.getChunk(x >> 4, z >> 4);
        int height;
        if (level.isDebug()) {
            height = level.getMaxY() + 1;
        } else if (chunk.hasPrimedHeightmap(Heightmap.Types.OCEAN_FLOOR)) {
            height = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) + 1;
        } else if (chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE)) {
            height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        } else {
            int highestSection = chunk.getHighestFilledSectionIndex();
            int highestY = highestSection == -1 ? chunk.getMinY()
                    : SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(highestSection));
            height = Math.min(level.getMaxY() + 1, highestY + 16);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, height - 1, z);
        while (pos.getY() >= level.getMinY()) {
            BlockState state = chunk.getBlockState(pos);
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)) return pos.getY() + 1;
            pos.move(net.minecraft.core.Direction.DOWN);
        }
        return level.getMinY();
    }

    static int roundedCoordinate(double value) {
        float input = (float) value;
        float rounded = Math.copySign((float) StrictMath.floor(Math.abs((double) input) + 0.5), input);
        return Float.isNaN(rounded) || rounded >= 0x1.0p31F || rounded < -0x1.0p31F
                ? Integer.MIN_VALUE : (int) rounded;
    }

    @Override public String getName() { return "query.above_top_solid"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
