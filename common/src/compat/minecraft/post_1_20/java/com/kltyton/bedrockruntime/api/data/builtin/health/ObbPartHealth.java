package com.kltyton.bedrockruntime.api.data.builtin.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * CN: KltytonBedrockRuntime 内置的单个 OBB 骨骼独立生命值。
 * EN: Built-in independent health value for one OBB bone.
 */
public record ObbPartHealth(float health, float maxHealth) {
    public static final Codec<ObbPartHealth> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("health").forGetter(ObbPartHealth::health),
            Codec.FLOAT.fieldOf("max_health").forGetter(ObbPartHealth::maxHealth))
            .apply(instance, ObbPartHealth::new));

    public ObbPartHealth {
        maxHealth = Math.max(0.0F, maxHealth);
        health = Math.clamp(health, 0.0F, maxHealth);
    }

    public ObbPartHealth hurt(float amount) {
        return new ObbPartHealth(health - Math.max(0.0F, amount), maxHealth);
    }

    public boolean broken() {
        return health <= 0.0F;
    }
}
