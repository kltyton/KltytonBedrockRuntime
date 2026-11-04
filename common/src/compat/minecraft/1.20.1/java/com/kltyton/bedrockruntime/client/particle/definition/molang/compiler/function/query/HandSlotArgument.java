package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

final class HandSlotArgument {
    private HandSlotArgument() {
    }

    static EquipmentSlot resolve(@Nullable MathValue argument, MolangInstance instance) {
        if (argument == null) return EquipmentSlot.MAINHAND;
        Object value = argument.getRaw(instance);
        if (value instanceof Number number) {
            if (number.doubleValue() == 0) return EquipmentSlot.MAINHAND;
            if (number.doubleValue() == 1) return EquipmentSlot.OFFHAND;
        } else if (value instanceof String name) {
            if (name.equals("main_hand")) return EquipmentSlot.MAINHAND;
            if (name.equals("off_hand")) return EquipmentSlot.OFFHAND;
        }
        throw new IllegalArgumentException("Invalid hand slot: " + value);
    }
}
