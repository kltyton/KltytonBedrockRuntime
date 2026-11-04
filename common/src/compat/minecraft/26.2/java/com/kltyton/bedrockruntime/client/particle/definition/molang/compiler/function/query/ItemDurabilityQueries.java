package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ContextValue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reads durability from the item supplied to the current render context. */
public final class ItemDurabilityQueries {
    private static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();
    private static final ContextValue RENDERED_ITEM = new ContextValue("context.rendered_item");

    private ItemDurabilityQueries() {
    }

    public static double maximum(MolangInstance instance) {
        ItemStack item = currentItem(instance, "query.max_durability");
        return item == null ? 0.0 : item.getMaxDamage();
    }

    public static double remaining(MolangInstance instance) {
        ItemStack item = currentItem(instance, "query.remaining_durability");
        if (item == null) return 0.0;
        int maximum = item.getMaxDamage();
        return maximum == 0 ? 0.0 : (float) maximum - item.getDamageValue();
    }

    private static @Nullable ItemStack currentItem(MolangInstance instance, String name) {
        if (instance.getReferencedItem() != null) return instance.getReferencedItem();
        Object supplied = RENDERED_ITEM.resolveProvided(instance);
        ItemStack item = supplied instanceof ItemStack stack ? stack : null;
        if (item == null && REPORTED_MISSING.add(name)) {
            BedrockParticles.LOGGER.warn("{} requires an item context", name);
        }
        return item;
    }
}
