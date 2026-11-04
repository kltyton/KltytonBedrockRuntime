package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.ItemAnimationFrameQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Advances actor-owned item frames when held items are rendered. */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Inject(method = "updateForTopItem", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$updateHeldItemFrame(ItemStackRenderState output, ItemStack item,
            ItemDisplayContext context, Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        if (level == null || !level.isClientSide() || owner == null) return;
        if (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return;
        if (!item.is(Items.CLOCK) && !item.is(Items.COMPASS) && !item.is(Items.RECOVERY_COMPASS)) return;
        var entity = owner.asLivingEntity();
        if (!(entity instanceof IEntity actor)) return;
        if (entity.isRemoved()) return;
        if (item.is(Items.CLOCK)) {
            actor.kltyton_bedrock_runtime$getClockAnimationFrame();
        } else if (item.is(Items.COMPASS)) {
            if (item.has(DataComponents.LODESTONE_TRACKER)) actor.kltyton_bedrock_runtime$getLodestoneCompassAnimationFrame(item);
            else actor.kltyton_bedrock_runtime$getCompassAnimationFrame();
        } else if (item.is(Items.RECOVERY_COMPASS) && ItemAnimationFrameQuery.recoveryStateAvailable(entity)) {
            actor.kltyton_bedrock_runtime$getRecoveryCompassAnimationFrame();
        }
    }
}
