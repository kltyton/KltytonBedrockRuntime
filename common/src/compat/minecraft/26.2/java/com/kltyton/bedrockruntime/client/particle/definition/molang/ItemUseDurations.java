package com.kltyton.bedrockruntime.client.particle.definition.molang;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.access.IItemUseTimer;

import java.util.Set;

/** Maps item use timers to the values exposed to Bedrock particle expressions. */
@ApiStatus.Internal
public final class ItemUseDurations {
    private static final Set<EntityType<?>> HUMANOID_MONSTERS = Set.of(
            EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.SKELETON, EntityTypes.STRAY,
            EntityTypes.WITHER_SKELETON, EntityTypes.BOGGED, EntityTypes.PARCHED,
            EntityTypes.WITCH, EntityTypes.VINDICATOR, EntityTypes.EVOKER, EntityTypes.PILLAGER,
            EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE
    );

    private ItemUseDurations() {}

    private static boolean isVanillaHumanoidMonster(LivingEntity entity) {
        return HUMANOID_MONSTERS.contains(entity.getType());
    }

    public static int maximum(ItemStack stack, LivingEntity entity) {
        if (stack.is(Items.CROSSBOW)) {
            int quickCharge = stack.getEnchantments().getLevel(entity.level().registryAccess().getOrThrow(Enchantments.QUICK_CHARGE));
            return quickCharge > 0 ? 25 - quickCharge * 5 : 25;
        }
        if (stack.is(Items.SPYGLASS)) return 72000;
        if (stack.is(Items.GOAT_HORN)) return 20;
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return 32;
        return stack.getUseDuration(entity);
    }

    public static boolean usesMappedTimer(ItemStack stack) {
        return stack.is(Items.CROSSBOW) || stack.is(Items.SPYGLASS) || stack.is(Items.GOAT_HORN);
    }

    public static int remaining(LivingEntity entity) {
        if (isVanillaHumanoidMonster(entity)) {
            ItemStack mainHand = entity.getMainHandItem();
            if (mainHand.is(Items.BOW) || mainHand.is(Items.SPYGLASS) || mainHand.is(Items.SHIELD)
                    || mainHand.is(Items.POTION) || mainHand.is(Items.SPLASH_POTION) || mainHand.is(Items.LINGERING_POTION)) return 0;
            if (mainHand.is(Items.CROSSBOW)) {
                boolean charged = CrossbowItem.isCharged(mainHand);
                if (charged && (entity.getType() == EntityTypes.PILLAGER || entity.getType() == EntityTypes.PIGLIN)) return 0;
                if (!charged && (!entity.isUsingItem()
                        || !entity.getItemInHand(entity.getUsedItemHand()).is(Items.CROSSBOW))) {
                    return maximum(mainHand, entity);
                }
            }
        }
        return remainingActive(entity);
    }

    /** Uses the active hand's timer, without idle main-hand compatibility values. */
    public static int remainingActive(LivingEntity entity) {
        int remaining = entity.getUseItemRemainingTicks();
        if (entity instanceof IItemUseTimer timer) {
            return timer.kltyton_bedrock_runtime$itemUseRemaining(remaining);
        }
        return remaining;
    }
}
