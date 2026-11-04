package com.kltyton.bedrockruntime.server.combat.damage;

import java.util.concurrent.atomic.AtomicBoolean;

import com.kltyton.bedrockruntime.api.data.builtin.key.ObbBuiltinDataKeys;
import com.kltyton.bedrockruntime.api.event.BoneHitboxEvents;

/**
 * CN: 内置逐部位生命值处理；仅扣除显式持有 PART_HEALTH 数据的受击骨骼。
 * EN: Built-in per-part health handling; only hurt bones explicitly carrying PART_HEALTH are damaged.
 */
public final class ObbPartHealthHooks {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ObbPartHealthHooks() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        BoneHitboxEvents.registerEntityHurtBoxHurt(context -> context.hurtBone()
                .getData(ObbBuiltinDataKeys.PART_HEALTH)
                .ifPresent(health -> context.hurtBone().setData(
                        ObbBuiltinDataKeys.PART_HEALTH,
                        health.hurt(context.damageReceived()))));
    }
}
