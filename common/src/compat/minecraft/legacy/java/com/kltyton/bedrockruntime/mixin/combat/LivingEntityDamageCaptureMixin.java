package com.kltyton.bedrockruntime.mixin.combat;

import java.util.ArrayDeque;
import java.util.Deque;

import com.kltyton.bedrockruntime.api.context.damage.ObbDamageInfo;
import com.kltyton.bedrockruntime.server.combat.damage.VanillaDamageCapture;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CN: 在公共原版伤害入口前后捕获请求伤害、生命值伤害和吸收伤害。
 * EN: Captures requested, health, and absorption damage around the common vanilla damage entrypoint.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageCaptureMixin {
    @Unique
    private final Deque<DamageFrame> kltyton_bedrock_runtime$damageFrames = new ArrayDeque<>();

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getAbsorptionAmount();

    @Inject(method = "hurt", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$beforeHurt(DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (((LivingEntity) (Object) this).level().isClientSide()) { return; }
        kltyton_bedrock_runtime$damageFrames.push(new DamageFrame(damage, getHealth(), getAbsorptionAmount()));
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$afterHurt(DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (((LivingEntity) (Object) this).level().isClientSide()) { return; }
        DamageFrame frame = kltyton_bedrock_runtime$damageFrames.poll();
        if (frame == null) {
            return;
        }
        VanillaDamageCapture.record(
                (LivingEntity) (Object) this,
                source,
                ObbDamageInfo.captured(
                        source,
                        frame.requestedDamage(),
                        frame.healthBefore() - getHealth(),
                        frame.absorptionBefore() - getAbsorptionAmount(),
                        callbackInfo.getReturnValue()));
    }

    @Unique
    private record DamageFrame(float requestedDamage, float healthBefore, float absorptionBefore) {
    }
}
