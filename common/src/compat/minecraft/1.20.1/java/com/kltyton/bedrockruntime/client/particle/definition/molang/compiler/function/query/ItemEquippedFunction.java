package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Checks the item held in the selected hand. */
public final class ItemEquippedFunction extends MathFunction {
    private final @Nullable MathValue hand;

    public ItemEquippedFunction(MathValue... values) {
        super(values);
        if (values.length > 1) throw new IllegalArgumentException("query.is_item_equipped accepts at most one argument");
        this.hand = values.length == 0 ? null : values[0];
    }

    public static double mainHand(MolangInstance instance) {
        return equipped(instance, EquipmentSlot.MAINHAND);
    }

    private static double equipped(MolangInstance instance, EquipmentSlot slot) {
        return instance.getAttachedEntity() instanceof LivingEntity entity
                && !entity.getItemBySlot(slot).isEmpty() ? 1.0 : 0.0;
    }

    @Override public double compute(MolangInstance instance) { return equipped(instance, HandSlotArgument.resolve(hand, instance)); }
    @Override public String getName() { return "query.is_item_equipped"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return hand == null ? new MathValue[0] : new MathValue[]{hand}; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
