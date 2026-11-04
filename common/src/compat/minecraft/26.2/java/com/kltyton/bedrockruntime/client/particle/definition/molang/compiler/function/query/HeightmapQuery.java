package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockHeightmap;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;

/** Reads the actor world's light-filtering column height without loading chunks. */
public final class HeightmapQuery extends MathFunction {
    private final MathValue[] arguments;

    public HeightmapQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!ActorAccess.hasBlockSource(instance) || arguments.length != 2) return 0.0;
        int x = AboveTopSolidQuery.roundedCoordinate(arguments[0].get(instance));
        int z = AboveTopSolidQuery.roundedCoordinate(arguments[1].get(instance));
        return BedrockHeightmap.height(instance.getLevel(), x, z);
    }

    @Override public String getName() { return "query.heightmap"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
