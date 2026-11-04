package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorSlotQuery extends MathFunction {
    public enum Kind { HAS_ARMOR, DAMAGE }

    private final Kind kind;
    private static final Set<Kind> REPORTED_INVALID_ARGUMENTS = ConcurrentHashMap.newKeySet();
    private final MathValue[] arguments;

    public ArmorSlotQuery(Kind kind, MathValue... values) {
        super(values);
        this.kind = kind;
        this.arguments = values.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length != 1) {
            if (REPORTED_INVALID_ARGUMENTS.add(kind)) {
                BedrockParticles.LOGGER.warn("Molang {} requires one armor slot index", getName());
            }
            return 0.0;
        }
        var actor = instance.getAttachedEntity();
        if (actor == null) return 0.0;
        float requested = (float) arguments[0].get(instance);
        int index;
        if (kind == Kind.HAS_ARMOR) {
            long direct = nativeLong(requested);
            long shifted = nativeLong(requested - 0x1.0p63F);
            index = (int) (direct | (direct >> 63) & shifted);
        } else {
            index = Float.isNaN(requested) || requested >= 0x1.0p31F || requested < -0x1.0p31F
                    ? Integer.MIN_VALUE : (int) requested;
        }
        if (!(actor instanceof LivingEntity entity)) return 0.0;
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

    private static long nativeLong(float value) {
        return Float.isNaN(value) || value >= 0x1.0p63F || value < -0x1.0p63F
                ? Long.MIN_VALUE : (long) value;
    }

    @Override public String getName() {
        return kind == Kind.HAS_ARMOR ? "query.has_armor_slot" : "query.armor_damage_slot";
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
