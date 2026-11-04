package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes native spell colors and identifiers without duplicating their table. */
@Mixin(SpellcasterIllager.IllagerSpell.class)
public interface SpellcasterIllagerSpellAccessor {
    @Accessor("spellColor")
    double[] kltyton_bedrock_runtime$spellColor();

    @Accessor("id")
    int kltyton_bedrock_runtime$spellId();
}
