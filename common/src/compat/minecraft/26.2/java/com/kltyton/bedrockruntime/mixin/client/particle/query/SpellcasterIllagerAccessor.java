package com.kltyton.bedrockruntime.mixin.client.particle.query;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads the active native spell with its exact method descriptor. */
@Mixin(SpellcasterIllager.class)
public interface SpellcasterIllagerAccessor {
    @Invoker("getCurrentSpell")
    SpellcasterIllager.IllagerSpell kltyton_bedrock_runtime$getCurrentSpell();
}
