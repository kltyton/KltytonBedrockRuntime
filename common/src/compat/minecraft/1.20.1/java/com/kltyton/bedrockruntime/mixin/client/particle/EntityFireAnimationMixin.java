package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.access.IEntityFireAnimation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tracks the fire animation state used by particle queries. */
@Mixin(Entity.class)
public abstract class EntityFireAnimationMixin implements IEntityFireAnimation {
    @Shadow @Final protected static EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;
    @Shadow private int remainingFireTicks;
    @Unique private boolean kltyton_bedrock_runtime$fireTracked;
    @Unique private boolean kltyton_bedrock_runtime$wasOnFire;
    @Unique private int kltyton_bedrock_runtime$fireStartTick;

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$updateFireAnimation(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide() || remainingFireTicks > 0 || kltyton_bedrock_runtime$fireTracked) {
            kltyton_bedrock_runtime$trackFireAnimation();
        }
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$updateFireAnimationData(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if (DATA_SHARED_FLAGS_ID.equals(accessor)) kltyton_bedrock_runtime$trackFireAnimation();
    }

    @Unique
    private void kltyton_bedrock_runtime$trackFireAnimation() {
        Entity entity = (Entity) (Object) this;
        boolean onFire = entity.isOnFire();
        if (onFire != kltyton_bedrock_runtime$wasOnFire) {
            kltyton_bedrock_runtime$fireTracked = true;
            kltyton_bedrock_runtime$fireStartTick = entity.tickCount;
        } else if (!onFire && kltyton_bedrock_runtime$fireTracked && entity.tickCount - kltyton_bedrock_runtime$fireStartTick > 4) {
            kltyton_bedrock_runtime$fireTracked = false;
        }
        kltyton_bedrock_runtime$wasOnFire = onFire;
    }

    @Override
    public int kltyton_bedrock_runtime$getFireAnimationTime() {
        kltyton_bedrock_runtime$trackFireAnimation();
        Entity entity = (Entity) (Object) this;
        return kltyton_bedrock_runtime$fireTracked ? entity.tickCount - kltyton_bedrock_runtime$fireStartTick : 0;
    }
}
