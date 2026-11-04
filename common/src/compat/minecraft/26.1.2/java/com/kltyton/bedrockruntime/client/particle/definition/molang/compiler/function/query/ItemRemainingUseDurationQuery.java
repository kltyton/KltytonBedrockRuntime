package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reads the actor use timer with the slot and normalization overloads. */
public final class ItemRemainingUseDurationQuery extends MathFunction {
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();
    private final MathValue[] arguments;
    private final boolean legacy;

    public ItemRemainingUseDurationQuery(MathValue... arguments) {
        this(MolangVersion.LATEST, arguments);
    }

    public ItemRemainingUseDurationQuery(MolangVersion version, MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
        this.legacy = !version.atLeast(1, 17, 30);
    }

    @Override
    public double compute(MolangInstance instance) {
        Entity actor = instance.getAttachedEntity();
        if (actor == null) return 0.0;
        if (arguments.length > 2) return contentError("takes at most two arguments");
        float remainingTicks = actor instanceof Player player ? ItemUseDurations.remaining(player) : 0.0F;
        if (arguments.length == 0) {
            return actor instanceof LivingEntity entity && !entity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                    ? remainingTicks * (legacy ? 20.0F : 0.05F) : 0.0;
        }

        Object first = arguments[0].getRaw(instance);
        String slotName = "main_hand";
        Float normalization = null;
        if (first instanceof String string) {
            slotName = string;
        } else if (arguments.length == 1 && first instanceof Number number) {
            normalization = number.floatValue();
        } else {
            return contentError("expected a string slot name or a numeric normalization value");
        }
        if (arguments.length == 2) {
            Object second = arguments[1].getRaw(instance);
            if (!(second instanceof Number number)) {
                return contentError("second argument must be a numeric normalization value");
            }
            normalization = number.floatValue();
        }

        EquipmentSlot slot;
        if (slotName.equals("main_hand")) slot = EquipmentSlot.MAINHAND;
        else if (slotName.equals("off_hand")) slot = EquipmentSlot.OFFHAND;
        else return contentError("slot must be main_hand or off_hand");
        if (!(actor instanceof LivingEntity entity)) return 0.0;
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return 0.0;
        // The slot-only overload retains its fixed tick multiplier.
        if (normalization == null) return remainingTicks * 20.0F;
        int duration = ItemUseDurations.maximum(stack, entity);
        if (duration <= 0) return 0.0;
        float fraction = Math.max(0.0F, Math.min(1.0F, remainingTicks / duration));
        if (legacy) fraction = 1.0F - fraction;
        return normalization * fraction;
    }

    private static double contentError(String message) {
        if (REPORTED_ERRORS.add(message)) {
            BedrockParticles.LOGGER.warn("query.item_remaining_use_duration: {}", message);
        }
        return 0.0;
    }

    @Override public String getName() { return "query.item_remaining_use_duration"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
