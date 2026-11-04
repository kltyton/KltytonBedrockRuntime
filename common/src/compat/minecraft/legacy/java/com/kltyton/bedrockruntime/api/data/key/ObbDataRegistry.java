package com.kltyton.bedrockruntime.api.data.key;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * CN: loader 无关的 OBB 扩展数据键注册表。应在实体存档加载前完成注册。
 * EN: Loader-neutral registry for OBB extension data keys. Register keys before entity save data is loaded.
 */
public final class ObbDataRegistry {
    private static final Map<ResourceLocation, ObbDataKey<?>> KEYS = new ConcurrentHashMap<>();

    private ObbDataRegistry() {
    }

    public static <T> ObbDataKey<T> register(ResourceLocation id, Codec<T> codec, Supplier<? extends T> defaultValue) {
        ObbDataKey<T> key = new ObbDataKey<>(id, codec, defaultValue);
        ObbDataKey<?> previous = KEYS.putIfAbsent(id, key);
        if (previous != null) {
            throw new IllegalStateException("Duplicate OBB data key: " + id);
        }
        return key;
    }

    public static Optional<ObbDataKey<?>> get(ResourceLocation id) {
        return Optional.ofNullable(KEYS.get(id));
    }
}
