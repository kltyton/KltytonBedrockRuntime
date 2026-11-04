package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.access.IMolangEntityState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Returns the main-hand item frame used by actor particle expressions. */
public final class ItemAnimationFrameQuery extends MathFunction {
    private static final Set<String> REPORTED_ITEMS = ConcurrentHashMap.newKeySet();
    private final MathValue[] arguments;

    public ItemAnimationFrameQuery(MathValue... arguments) {
        super(arguments);
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return 0.0;
        ItemStack item = entity.getMainHandItem();
        if (item.isEmpty()) return 0.0;
        if (item.is(Items.BOW)) return bowFrame(item, entity);
        if (item.is(Items.CROSSBOW)) return crossbowFrame(item, entity);
        if (item.is(Items.FISHING_ROD)) {
            return !entity.isRemoved() && entity instanceof Player player && player.fishing != null ? 1.0 : 0.0;
        }
        if (item.is(Items.WOLF_ARMOR)) return item.has(DataComponents.DYED_COLOR) ? 1.0 : 0.0;
        if ((item.is(Items.CLOCK) || item.is(Items.COMPASS) || item.is(Items.RECOVERY_COMPASS))
                && entity.isRemoved()) return 0.0;
        if (item.is(Items.CLOCK) && entity.level().isClientSide() && entity instanceof IEntity actor) {
            return actor.kltyton_bedrock_runtime$getClockAnimationFrame();
        }
        if (item.is(Items.COMPASS) && !item.has(DataComponents.LODESTONE_TRACKER)
                && entity.level().isClientSide() && entity instanceof IEntity actor) {
            return actor.kltyton_bedrock_runtime$getCompassAnimationFrame();
        }
        if (item.is(Items.COMPASS) && item.has(DataComponents.LODESTONE_TRACKER)
                && entity.level().isClientSide() && entity instanceof IEntity actor) {
            return actor.kltyton_bedrock_runtime$getLodestoneCompassAnimationFrame(item);
        }
        if (item.is(Items.RECOVERY_COMPASS) && recoveryStateAvailable(entity)
                && entity.level().isClientSide() && entity instanceof IEntity actor) {
            return actor.kltyton_bedrock_runtime$getRecoveryCompassAnimationFrame();
        }

        var id = BuiltInRegistries.ITEM.getKey(item.getItem());
        if (item.is(Items.CLOCK) || item.is(Items.COMPASS) || item.is(Items.RECOVERY_COMPASS)
                || !id.getNamespace().equals("minecraft")) {
            String name = id.toString();
            if (REPORTED_ITEMS.add(name)) {
                BedrockParticles.LOGGER.warn("No particle animation frame mapping for item {}", name);
            }
        }
        return 0.0;
    }

    public static boolean recoveryStateAvailable(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.isLocalPlayer() || player.getLastDeathLocation().isPresent()
                    || entity instanceof IMolangEntityState state && state.kltyton_bedrock_runtime$recoveryStateKnown();
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("minecraft");
    }

    private static int bowFrame(ItemStack item, LivingEntity entity) {
        if (entity.isRemoved()) return 0;
        int remaining = ItemUseDurations.remaining(entity);
        if (remaining == 0) return 0;
        float elapsed = (float) (ItemUseDurations.maximum(item, entity) - remaining) / 20.0F;
        float strength = (elapsed + elapsed + elapsed * elapsed) / 3.0F;
        strength = strength <= 1.0F ? strength : 1.0F;
        return nativeInt(3.0F * strength * 0.99F) + 1;
    }

    private static int crossbowFrame(ItemStack item, LivingEntity entity) {
        if (entity.isRemoved()) return chargedFrame(item);
        int remaining = ItemUseDurations.remaining(entity);
        if (remaining < 1) return chargedFrame(item);

        int duration = ItemUseDurations.maximum(item, entity);
        float elapsed = (float) (duration - remaining) / (float) duration;
        float strength = (elapsed + elapsed + elapsed * elapsed) / 3.0F;
        strength = strength <= 1.0F ? strength : 1.0F;
        if (strength >= 1.0F) {
            int loadedFrame = chargedFrame(item);
            if (loadedFrame != 0) return loadedFrame;
        }
        int frame = nativeInt(strength * 0.99F * 5.0F);
        if (strength < 1.0F && frame > 3) {
            return entity.getOffhandItem().is(Items.FIREWORK_ROCKET) ? 5 : 4;
        }
        return frame;
    }

    private static int chargedFrame(ItemStack item) {
        ChargedProjectiles projectiles = item.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (projectiles.isEmpty()) return 0;
        ItemStack projectile = projectiles.getItems().get(0);
        return projectile.is(Items.ARROW) || projectile.is(Items.TIPPED_ARROW) || projectile.is(Items.SPECTRAL_ARROW) ? 4 : 5;
    }

    private static int nativeInt(float value) {
        return Float.isNaN(value) || value >= 0x1.0p31F || value < -0x1.0p31F ? Integer.MIN_VALUE : (int) value;
    }

    @Override public String getName() { return "query.get_animation_frame"; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
