package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.kltyton.bedrockruntime.client.particle.definition.molang.ItemUseDurations;
import com.kltyton.bedrockruntime.client.particle.access.IItemUseTimer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records item use timing for particle queries on each side. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseTimerMixin implements IItemUseTimer {
    @Unique private int kltyton_bedrock_runtime$javaTimerStart;
    @Unique private int kltyton_bedrock_runtime$bedrockTimerStart;
    @Unique private boolean kltyton_bedrock_runtime$usingMappedTimer;
    @Unique private boolean kltyton_bedrock_runtime$remoteItemUseKnown;
    @Unique private boolean kltyton_bedrock_runtime$remoteItemUseActive;
    @Unique private int kltyton_bedrock_runtime$remoteItemUseRemaining;
    @Unique private int kltyton_bedrock_runtime$remoteItemUseTick;

    @Inject(method = "startUsingItem", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;useItemRemaining:I",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void kltyton_bedrock_runtime$beginItemUse(InteractionHand hand, CallbackInfo ci) {
        kltyton_bedrock_runtime$captureItemUse();
    }

    @Inject(method = "onSyncedDataUpdated", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;useItemRemaining:I",
            opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void kltyton_bedrock_runtime$beginSyncedItemUse(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        kltyton_bedrock_runtime$captureItemUse();
    }

    @Unique
    private void kltyton_bedrock_runtime$captureItemUse() {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack item = entity.getUseItem();
        if (entity.level().isClientSide() && kltyton_bedrock_runtime$remoteItemUseKnown && !kltyton_bedrock_runtime$remoteItemUseActive) {
            kltyton_bedrock_runtime$remoteItemUseKnown = false;
        }
        kltyton_bedrock_runtime$usingMappedTimer = ItemUseDurations.usesMappedTimer(item);
        if (kltyton_bedrock_runtime$usingMappedTimer) {
            kltyton_bedrock_runtime$javaTimerStart = entity.getUseItemRemainingTicks();
            kltyton_bedrock_runtime$bedrockTimerStart = ItemUseDurations.maximum(item, entity);
        }
    }

    @Override
    public void kltyton_bedrock_runtime$setRemoteItemUseState(boolean active, int remaining) {
        kltyton_bedrock_runtime$remoteItemUseKnown = true;
        kltyton_bedrock_runtime$remoteItemUseActive = active;
        kltyton_bedrock_runtime$remoteItemUseRemaining = remaining;
        kltyton_bedrock_runtime$remoteItemUseTick = ((LivingEntity) (Object) this).tickCount;
    }

    @Override
    public int kltyton_bedrock_runtime$itemUseRemaining(int javaRemaining) {
        if (kltyton_bedrock_runtime$remoteItemUseKnown && kltyton_bedrock_runtime$remoteItemUseActive) {
            int remaining = kltyton_bedrock_runtime$remoteItemUseRemaining;
            int elapsed = ((LivingEntity) (Object) this).tickCount - kltyton_bedrock_runtime$remoteItemUseTick;
            return remaining > 0 ? Math.max(0, remaining - elapsed) : remaining;
        }
        if (kltyton_bedrock_runtime$remoteItemUseKnown
                && (kltyton_bedrock_runtime$usingMappedTimer || (Object) this instanceof Player)) return 0;
        if (!kltyton_bedrock_runtime$usingMappedTimer || javaRemaining <= 0) return javaRemaining;
        if (kltyton_bedrock_runtime$bedrockTimerStart <= 0) return kltyton_bedrock_runtime$bedrockTimerStart;
        return Math.max(0, kltyton_bedrock_runtime$bedrockTimerStart - (kltyton_bedrock_runtime$javaTimerStart - javaRemaining));
    }
}
