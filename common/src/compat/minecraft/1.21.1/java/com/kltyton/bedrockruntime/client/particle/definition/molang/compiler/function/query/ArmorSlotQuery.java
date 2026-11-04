package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

public final class ArmorSlotQuery extends MathFunction {
    public enum Kind { HAS_ARMOR, DAMAGE }

    private final Kind kind;
    private final MathValue slot;

    public ArmorSlotQuery(Kind kind, MathValue... values) {
        super(values);
        this.kind = kind;
        this.slot = values[0];
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return 0.0;
        int index = (int) slot.get(instance);
        EquipmentSlot equipment = switch (index) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            case 4 -> kind == Kind.DAMAGE ? EquipmentSlot.BODY : null;
            default -> null;
        };
        if (equipment == null) return 0.0;
        ItemStack stack = entity.getItemBySlot(equipment);
        if (stack.isEmpty()) return 0.0;
        return kind == Kind.HAS_ARMOR ? 1.0 : stack.getDamageValue();
    }

    @Override public String getName() {
        return kind == Kind.HAS_ARMOR ? "query.has_armor_slot" : "query.armor_damage_slot";
    }
    @Override public int getMinArgs() { return 1; }
    @Override public void validate(MathValue... values) {
        if (values.length != 1) throw new IllegalArgumentException("Armor slot query requires one index");
    }
    @Override public MathValue[] getArgs() { return new MathValue[]{slot}; }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
