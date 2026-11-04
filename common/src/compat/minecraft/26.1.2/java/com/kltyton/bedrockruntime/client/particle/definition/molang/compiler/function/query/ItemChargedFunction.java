package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Reads the charged-projectiles component of the selected hand. */
public final class ItemChargedFunction extends MathFunction {
    private final MathValue[] arguments;
    private final @Nullable MathValue hand;

    public ItemChargedFunction(MathValue... values) {
        super(values);
        this.arguments = values.clone();
        this.hand = arguments.length == 0 ? null : arguments[0];
    }

    public static double mainHand(MolangInstance instance) {
        return charged(instance, EquipmentSlot.MAINHAND);
    }

    private static double charged(MolangInstance instance, EquipmentSlot slot) {
        return instance.getAttachedEntity() instanceof LivingEntity entity
                && CrossbowItem.isCharged(entity.getItemBySlot(slot)) ? 1.0 : 0.0;
    }

    @Override
    public double compute(MolangInstance instance) {
        if (instance.getAttachedEntity() == null) return 0.0;
        EquipmentSlot slot = HandSlotArgument.resolve(hand, instance);
        return slot == null ? 0.0 : charged(instance, slot);
    }

    @Override public String getName() { return "query.item_is_charged"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
