package com.kltyton.bedrockruntime.mixin.client.geckolib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.core.molang.LazyVariable;

import java.util.function.DoubleSupplier;

/** Narrow accessor for GeckoLib 4.8.4's private Molang value supplier. */
@Mixin(value = LazyVariable.class, remap = false)
public interface GeckoLazyVariableAccessor {
    @Accessor("valueSupplier")
    DoubleSupplier kltyton_bedrock_runtime$getValueSupplier();
}
