package com.kltyton.bedrockruntime.api.data.builtin.key;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.data.builtin.health.ObbPartHealth;
import com.kltyton.bedrockruntime.api.data.key.ObbDataKey;
import com.kltyton.bedrockruntime.api.data.key.ObbDataRegistry;
import com.mojang.serialization.Codec;

/**
 * CN: KltytonBedrockRuntime 自带、可持久化的骨骼扩展数据键。
 * EN: Persistent bone extension-data keys built into KltytonBedrockRuntime.
 */
public final class ObbBuiltinDataKeys {
    /**
     * CN: 为 true 时，该硬碰撞 OBB 可承载站在其上方的实体，并传递自身平移/旋转位移。
     * EN: When true, this hard-collision OBB carries entities standing on it and transfers translation/rotation motion.
     */
    public static final ObbDataKey<Boolean> CARRIES_ENTITIES = ObbDataRegistry.register(
            Constants.id("carries_entities"),
            Codec.BOOL,
            () -> false);

    /**
     * CN: 每个 OBB 骨骼独立持有的内置部位生命值。
     * EN: Built-in part health stored independently by every OBB bone.
     */
    public static final ObbDataKey<ObbPartHealth> PART_HEALTH = ObbDataRegistry.register(
            Constants.id("part_health"),
            ObbPartHealth.CODEC,
            () -> new ObbPartHealth(20.0F, 20.0F));

    private ObbBuiltinDataKeys() {
    }

    /** CN: 触发内置键注册。EN: Triggers built-in key registration. */
    public static void init() {
    }
}
