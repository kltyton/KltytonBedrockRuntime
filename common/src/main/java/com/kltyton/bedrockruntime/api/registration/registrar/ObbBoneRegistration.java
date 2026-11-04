package com.kltyton.bedrockruntime.api.registration.registrar;

import com.kltyton.bedrockruntime.api.registration.definition.ObbBoneDefinition;
import com.kltyton.bedrockruntime.api.registration.selector.ObbBoneSelector;
import com.kltyton.bedrockruntime.api.registration.selector.ObbBoneSelectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.data.key.ObbDataKey;
import com.kltyton.bedrockruntime.api.registration.data.ObbBoneDataDefinition;

/**
 * CN: 一条骨骼注册的链式配置对象。
 * EN: Fluent configuration object for one bone registration.
 */
public final class ObbBoneRegistration {
    private final ObbBoneSelector selector;
    private final EnumSet<ObbBoneAttribute> attributes = EnumSet.noneOf(ObbBoneAttribute.class);
    private final List<ObbBoneDataDefinition> dataDefinitions = new ArrayList<>();
    private ObbCollisionMode collisionMode = ObbCollisionMode.NONE;

    ObbBoneRegistration(ObbBoneSelector selector) {
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    public ObbBoneRegistration attributes(ObbBoneAttribute... values) {
        if (values != null) {
            for (ObbBoneAttribute value : values) {
                attributes.add(Objects.requireNonNull(value, "attribute"));
            }
        }
        return this;
    }

    public ObbBoneRegistration attack() {
        return attributes(ObbBoneAttribute.ATTACK);
    }

    /** CN: 可选的永久 HURT 初值；攻击接触目标不需要预先注册它。EN: Optional permanent HURT default; attack-contact targets need not register it. */
    public ObbBoneRegistration hurt() {
        return attributes(ObbBoneAttribute.HURT);
    }

    public ObbBoneRegistration collision(ObbCollisionMode mode) {
        collisionMode = Objects.requireNonNull(mode, "mode");
        if (mode != ObbCollisionMode.NONE) {
            attributes.add(ObbBoneAttribute.COLLISION);
        }
        return this;
    }

    /** CN: 为当前 register 选中的全部 OBB 独立创建该键的默认值。EN: Creates the key's default independently for every OBB selected by the current register call. */
    public <T> ObbBoneRegistration data(ObbDataKey<T> key) {
        return addDataFactory(ObbBoneSelectors.ALL, key, key::createDefaultValue);
    }

    /**
     * CN: 为每个匹配骨骼设置一个初值。值应为不可变对象；可变对象请使用 dataFactory。
     * EN: Sets an initial value for every matched bone. Use dataFactory for mutable values.
     */
    public <T> ObbBoneRegistration data(ObbDataKey<T> key, T initialValue) {
        Objects.requireNonNull(initialValue, "initialValue");
        return addDataFactory(ObbBoneSelectors.ALL, key, () -> initialValue);
    }

    /**
     * CN: 在当前 register 范围内进一步选择一块或多块 OBB，并为每块独立调用数据工厂。
     * EN: Selects one or more OBBs within the current register scope and invokes the data factory independently for each.
     */
    public <T> ObbBoneRegistration dataFactory(ObbBoneSelector dataSelector, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        return addDataFactory(dataSelector, key, factory);
    }

    public <T> ObbBoneRegistration dataFactory(String boneName, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        return addDataFactory(ObbBoneSelectors.named(boneName), key, factory);
    }

    public <T> ObbBoneRegistration dataFactory(String source, String boneName, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        return addDataFactory(ObbBoneSelectors.named(source, boneName), key, factory);
    }

    /** CN: 在当前注册范围内按名称选择任意一块或多块 OBB。EN: Selects any one or more named OBBs inside the current registration scope. */
    public <T> ObbBoneRegistration dataFactory(Collection<String> boneNames, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        return addDataFactory(ObbBoneSelectors.namedAny(boneNames), key, factory);
    }

    public <T> ObbBoneRegistration dataFactory(ObbBoneKey bone, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        return addDataFactory(ObbBoneSelectors.exact(bone), key, factory);
    }

    private <T> ObbBoneRegistration addDataFactory(ObbBoneSelector dataSelector, ObbDataKey<T> key,
            Supplier<? extends T> factory) {
        ObbBoneSelector safeSelector = Objects.requireNonNull(dataSelector, "dataSelector");
        ObbDataKey<T> safeKey = Objects.requireNonNull(key, "key");
        Supplier<? extends T> safeFactory = Objects.requireNonNull(factory, "factory");
        dataDefinitions.add(new ObbBoneDataDefinition(safeSelector, safeKey, safeFactory));
        return this;
    }

    ObbBoneDefinition freeze() {
        return new ObbBoneDefinition(selector, attributes, collisionMode, dataDefinitions);
    }
}
