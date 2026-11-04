package com.kltyton.bedrockruntime.api.data.key;

import java.util.Objects;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * CN: 一个可持久化 OBB 骨骼扩展值的类型化键。
 * EN: Typed key for one persistent OBB-bone extension value.
 */
public final class ObbDataKey<T> {
    private final ResourceLocation id;
    private final Codec<T> codec;
    private final Supplier<? extends T> defaultValue;

    ObbDataKey(ResourceLocation id, Codec<T> codec, Supplier<? extends T> defaultValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    public ResourceLocation id() {
        return id;
    }

    public Codec<T> codec() {
        return codec;
    }

    public T createDefaultValue() {
        return Objects.requireNonNull(defaultValue.get(), () -> "Default value is null for " + id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
