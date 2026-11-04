package com.kltyton.bedrockruntime.api.registration.data;

import java.util.Objects;
import java.util.function.Supplier;

import com.kltyton.bedrockruntime.api.data.key.ObbDataKey;
import com.kltyton.bedrockruntime.api.registration.selector.ObbBoneSelector;

/**
 * CN: 一条冻结后的、相对于父骨骼注册范围的数据绑定。
 * EN: Frozen data binding whose selector is evaluated within its parent bone registration.
 */
public record ObbBoneDataDefinition(
        ObbBoneSelector selector,
        ObbDataKey<?> key,
        Supplier<?> factory) {
    public ObbBoneDataDefinition {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");
    }

    public Object createValue() {
        return Objects.requireNonNull(factory.get(), () -> "OBB data factory returned null for " + key.id());
    }
}
