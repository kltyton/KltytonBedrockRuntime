package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.LivingEntity;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class ItemUseDurationQuery extends MathFunction {
    public ItemUseDurationQuery(MathValue... values) {
        super(values);
        if (values.length != 0) throw new IllegalArgumentException("query.item_in_use_duration takes no arguments");
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!(instance.getAttachedEntity() instanceof LivingEntity entity) || !entity.isUsingItem()) return 0.0;
        float ticks = entity.getTicksUsingItem() + RenderFrameContext.currentOr(0.0F);
        return Math.min(ticks, entity.getUseItem().getUseDuration(entity)) * com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock.nominalTickSeconds(instance.getLevel());
    }

    @Override public String getName() { return "query.item_in_use_duration"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return new MathValue[0]; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
