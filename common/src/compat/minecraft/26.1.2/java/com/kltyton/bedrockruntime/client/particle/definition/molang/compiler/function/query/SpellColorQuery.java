package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import com.kltyton.bedrockruntime.mixin.client.particle.query.SpellcasterIllagerAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.query.SpellcasterIllagerSpellAccessor;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

public final class SpellColorQuery extends MathFunction {
    private final MathValue[] arguments;

    public SpellColorQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        MolangStruct color = new MolangStruct();
        double[] channels = null;
        boolean active = false;
        if (instance.getAttachedEntity() instanceof SpellcasterIllager caster) {
            SpellcasterIllagerSpellAccessor spell = (SpellcasterIllagerSpellAccessor) (Object)
                    ((SpellcasterIllagerAccessor) caster).kltyton_bedrock_runtime$getCurrentSpell();
            channels = spell.kltyton_bedrock_runtime$spellColor();
            active = spell.kltyton_bedrock_runtime$spellId() != 0;
        }
        color.put("r", channel(channels, 0));
        color.put("g", channel(channels, 1));
        color.put("b", channel(channels, 2));
        color.put("a", active ? 1.0 : 0.0);
        return color;
    }

    private static float channel(double[] channels, int index) {
        return channels == null ? 0.0F : Math.round((float) channels[index] * 255.0F) / 255.0F;
    }

    @Override public double compute(MolangInstance instance) {
        getRaw(instance);
        return 0.0;
    }
    @Override public boolean isMutable(MathValue... values) { return true; }
    @Override public String getName() { return "query.spellcolor"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
}
