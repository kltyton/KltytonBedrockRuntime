package com.kltyton.bedrockruntime.api.context.damage;

import java.util.Optional;

import net.minecraft.world.damagesource.DamageSource;

/**
 * CN: 从原版 LivingEntity 伤害入口捕获的伤害结果。
 * EN: Damage result captured from the vanilla LivingEntity damage entrypoint.
 *
 * @param source 原版伤害源；纯 OBB 接触事件为空 / vanilla damage source; empty for OBB-only contacts
 * @param requestedDamage 进入 LivingEntity.hurt 的伤害，尚未经过护甲、吸收等减免 / damage entering LivingEntity.hurt before armor and absorption
 * @param healthDamage 实际扣除的生命值 / health actually removed
 * @param absorptionDamage 实际扣除的吸收值 / absorption actually removed
 * @param successful 原版 hurt 是否返回成功 / whether vanilla hurt returned success
 */
public record ObbDamageInfo(
        Optional<DamageSource> source,
        float requestedDamage,
        float healthDamage,
        float absorptionDamage,
        boolean successful) {
    public static final ObbDamageInfo NONE = new ObbDamageInfo(Optional.empty(), 0.0F, 0.0F, 0.0F, false);

    public ObbDamageInfo {
        source = source == null ? Optional.empty() : source;
        requestedDamage = finiteNonNegative(requestedDamage);
        healthDamage = finiteNonNegative(healthDamage);
        absorptionDamage = finiteNonNegative(absorptionDamage);
    }

    public static ObbDamageInfo captured(DamageSource source, float requestedDamage, float healthDamage,
            float absorptionDamage, boolean successful) {
        return new ObbDamageInfo(Optional.ofNullable(source), requestedDamage, healthDamage, absorptionDamage, successful);
    }

    /** CN: 生命值与吸收值合计损失。EN: Combined health and absorption loss. */
    public float appliedDamage() {
        return healthDamage + absorptionDamage;
    }

    public boolean available() {
        return source.isPresent();
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }
}
