package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
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
        if (index == 4 && kind == Kind.DAMAGE) {
            ItemStack body = bodyArmor(entity);
            return body.isEmpty() ? 0.0 : body.getDamageValue();
        }
        EquipmentSlot equipment = switch (index) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> null;
        };
        if (equipment == null) return 0.0;
        ItemStack stack = entity.getItemBySlot(equipment);
        if (stack.isEmpty()) return 0.0;
        return kind == Kind.HAS_ARMOR ? 1.0 : stack.getDamageValue();
    }

    private static ItemStack bodyArmor(LivingEntity entity) {
        return entity instanceof Horse horse ? horse.getArmor() : ItemStack.EMPTY;
    }

    public static boolean hasBodyEquipment(LivingEntity entity) {
        return !bodyArmor(entity).isEmpty() || entity instanceof Llama llama && llama.getSwag() != null;
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
