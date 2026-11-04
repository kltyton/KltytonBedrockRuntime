package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class HandSlotArgument {
    private static final Set<String> REPORTED_INVALID = ConcurrentHashMap.newKeySet();
    private HandSlotArgument() {
    }

    static @Nullable EquipmentSlot resolve(@Nullable MathValue argument, MolangInstance instance) {
        if (argument == null) return EquipmentSlot.MAINHAND;
        Object value = argument.getRaw(instance);
        if (value instanceof Number number) {
            float numeric = number.floatValue();
            if (numeric > -1.0F && numeric < 1.0F) return EquipmentSlot.MAINHAND;
            if (numeric >= 1.0F && numeric < 2.0F) return EquipmentSlot.OFFHAND;
        } else if (value instanceof String name) {
            if (name.equals("main_hand")) return EquipmentSlot.MAINHAND;
            if (name.equals("off_hand")) return EquipmentSlot.OFFHAND;
        }
        String key = String.valueOf(value);
        if (REPORTED_INVALID.add(key)) BedrockParticles.LOGGER.warn("Invalid Molang hand slot: {}", key);
        return null;
    }
}
