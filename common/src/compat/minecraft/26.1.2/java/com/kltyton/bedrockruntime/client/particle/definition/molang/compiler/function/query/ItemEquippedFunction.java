package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Checks the item held in the selected hand. */
public final class ItemEquippedFunction extends MathFunction {
    private final MathValue[] arguments;
    private final @Nullable MathValue hand;

    public ItemEquippedFunction(MathValue... values) {
        super(values);
        this.arguments = values.clone();
        this.hand = arguments.length == 0 ? null : arguments[0];
    }

    public static double mainHand(MolangInstance instance) {
        return equipped(instance, EquipmentSlot.MAINHAND);
    }

    private static double equipped(MolangInstance instance, EquipmentSlot slot) {
        return instance.getAttachedEntity() instanceof LivingEntity entity
                && !entity.getItemBySlot(slot).isEmpty() ? 1.0 : 0.0;
    }

    @Override public double compute(MolangInstance instance) {
        if (instance.getAttachedEntity() == null) return 0.0;
        EquipmentSlot slot = HandSlotArgument.resolve(hand, instance);
        return slot == null ? 0.0 : equipped(instance, slot);
    }
    @Override public String getName() { return "query.is_item_equipped"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
