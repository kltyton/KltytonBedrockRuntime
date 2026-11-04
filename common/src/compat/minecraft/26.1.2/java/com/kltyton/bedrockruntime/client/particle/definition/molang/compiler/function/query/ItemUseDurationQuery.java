package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.LivingEntity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class ItemUseDurationQuery extends MathFunction {
    private final MathValue[] arguments;

    public ItemUseDurationQuery(MathValue... values) {
        super(values);
        this.arguments = values.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return 0.0;
        var mainHand = entity.getMainHandItem();
        if (mainHand.isEmpty()) return 0.0;
        int remaining = ItemUseDurations.remaining(entity);
        return (float) (ItemUseDurations.maximum(mainHand, entity) - remaining) * 20.0F;
    }

    @Override public String getName() { return "query.item_in_use_duration"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
