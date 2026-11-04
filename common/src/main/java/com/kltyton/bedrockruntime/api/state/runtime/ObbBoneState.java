package com.kltyton.bedrockruntime.api.state.runtime;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.bone.collision.ObbCollisionMode;
import com.kltyton.bedrockruntime.api.data.builtin.key.ObbBuiltinDataKeys;
import com.kltyton.bedrockruntime.api.data.key.ObbDataKey;

/**
 * CN: 单个已发现 OBB 骨骼的运行时状态。持久属性、动态属性和客户端快照属性会叠加。
 * EN: Runtime state of one discovered OBB bone. Persistent, dynamic, and client-snapshot attributes are composed.
 */
public final class ObbBoneState {
    private final ObbBoneKey key;
    private final EnumSet<ObbBoneAttribute> registeredAttributes;
    private final EnumSet<ObbBoneAttribute> persistentAttributes = EnumSet.noneOf(ObbBoneAttribute.class);
    private final EnumSet<ObbBoneAttribute> disabledAttributes = EnumSet.noneOf(ObbBoneAttribute.class);
    private final EnumSet<ObbBoneAttribute> persistentGroupAttributes = EnumSet.noneOf(ObbBoneAttribute.class);
    private final Map<String, EnumSet<ObbBoneAttribute>> temporaryAttributes = new LinkedHashMap<>();
    private final Map<String, ObbCollisionMode> temporaryCollisionModes = new LinkedHashMap<>();
    private final EnumSet<ObbBoneAttribute> clientSnapshotAttributes = EnumSet.noneOf(ObbBoneAttribute.class);
    private final Map<ObbDataKey<?>, Object> data = new LinkedHashMap<>();
    private final ObbCollisionMode registeredCollisionMode;
    private ObbCollisionMode persistentCollisionMode;
    private ObbCollisionMode clientCollisionMode = ObbCollisionMode.NONE;
    private boolean hasClientSnapshot;

    ObbBoneState(ObbBoneKey key, Set<ObbBoneAttribute> attributes, ObbCollisionMode collisionMode,
            Map<ObbDataKey<?>, Object> defaultData) {
        this.key = key;
        this.registeredAttributes = copyAttributes(attributes);
        this.registeredCollisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
        this.data.putAll(defaultData);
    }

    public static ObbBoneState synthetic(ObbBoneKey key, ObbBoneAttribute... attributes) {
        EnumSet<ObbBoneAttribute> values = EnumSet.noneOf(ObbBoneAttribute.class);
        if (attributes != null) {
            for (ObbBoneAttribute attribute : attributes) {
                values.add(attribute);
            }
        }
        return new ObbBoneState(key, values, ObbCollisionMode.NONE, Map.of());
    }

    public ObbBoneKey key() {
        return key;
    }

    public synchronized Set<ObbBoneAttribute> attributes() {
        EnumSet<ObbBoneAttribute> result = EnumSet.copyOf(registeredAttributes);
        result.addAll(persistentAttributes);
        result.addAll(persistentGroupAttributes);
        if (hasClientSnapshot) {
            result.addAll(clientSnapshotAttributes);
        }
        result.removeAll(disabledAttributes);
        temporaryAttributes.values().forEach(result::addAll);
        return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }

    public synchronized boolean hasAttribute(ObbBoneAttribute attribute) {
        Objects.requireNonNull(attribute, "attribute");
        if (temporaryAttributes.values().stream().anyMatch(values -> values.contains(attribute))) {
            return true;
        }
        return !disabledAttributes.contains(attribute)
                && (registeredAttributes.contains(attribute)
                        || persistentAttributes.contains(attribute)
                        || persistentGroupAttributes.contains(attribute)
                        || hasClientSnapshot && clientSnapshotAttributes.contains(attribute));
    }

    /** CN: 永久设置一个可组合属性。EN: Permanently sets one composable attribute. */
    public synchronized void setAttribute(ObbBoneAttribute attribute, boolean enabled) {
        Objects.requireNonNull(attribute, "attribute");
        if (enabled) {
            disabledAttributes.remove(attribute);
            persistentAttributes.add(attribute);
        } else {
            persistentAttributes.remove(attribute);
            disabledAttributes.add(attribute);
        }
    }

    public boolean isAttackBox() {
        return hasAttribute(ObbBoneAttribute.ATTACK);
    }

    public void setAttackBox(boolean enabled) {
        setAttribute(ObbBoneAttribute.ATTACK, enabled);
    }

    public boolean isHurtBox() {
        return hasAttribute(ObbBoneAttribute.HURT);
    }

    public void setHurtBox(boolean enabled) {
        setAttribute(ObbBoneAttribute.HURT, enabled);
    }

    public boolean isCollisionBox() {
        return hasAttribute(ObbBoneAttribute.COLLISION);
    }

    public void setCollisionBox(boolean enabled) {
        setAttribute(ObbBoneAttribute.COLLISION, enabled);
    }

    /**
     * CN: 在命名作用域中临时叠加属性；清除该作用域后会自动恢复此前状态。
     * EN: Temporarily overlays an attribute in a named scope; clearing the scope restores the previous state.
     */
    public synchronized void setTemporaryAttribute(String scope, ObbBoneAttribute attribute, boolean enabled) {
        String safeScope = requireScope(scope);
        Objects.requireNonNull(attribute, "attribute");
        EnumSet<ObbBoneAttribute> values = temporaryAttributes.computeIfAbsent(
                safeScope,
                ignored -> EnumSet.noneOf(ObbBoneAttribute.class));
        if (enabled) {
            values.add(attribute);
        } else {
            values.remove(attribute);
            if (attribute == ObbBoneAttribute.COLLISION) {
                temporaryCollisionModes.remove(safeScope);
            }
        }
        if (values.isEmpty()) {
            temporaryAttributes.remove(safeScope);
            temporaryCollisionModes.remove(safeScope);
        }
    }

    public synchronized boolean hasTemporaryAttribute(String scope, ObbBoneAttribute attribute) {
        EnumSet<ObbBoneAttribute> values = temporaryAttributes.get(requireScope(scope));
        return values != null && values.contains(Objects.requireNonNull(attribute, "attribute"));
    }

    public synchronized Set<ObbBoneAttribute> temporaryAttributes(String scope) {
        EnumSet<ObbBoneAttribute> values = temporaryAttributes.get(requireScope(scope));
        return values == null || values.isEmpty() ? Set.of() : Set.copyOf(values);
    }

    public synchronized void clearTemporaryAttributes(String scope) {
        String safeScope = requireScope(scope);
        temporaryAttributes.remove(safeScope);
        temporaryCollisionModes.remove(safeScope);
    }

    public void setTemporaryAttackBox(String scope, boolean enabled) {
        setTemporaryAttribute(scope, ObbBoneAttribute.ATTACK, enabled);
    }

    public void setTemporaryHurtBox(String scope, boolean enabled) {
        setTemporaryAttribute(scope, ObbBoneAttribute.HURT, enabled);
    }

    /** CN: 临时启用指定物理模式的碰撞属性。EN: Temporarily enables collision with the requested physical mode. */
    public synchronized void setTemporaryCollision(String scope, ObbCollisionMode mode) {
        String safeScope = requireScope(scope);
        ObbCollisionMode safeMode = mode == null ? ObbCollisionMode.NONE : mode;
        setTemporaryAttribute(safeScope, ObbBoneAttribute.COLLISION, safeMode != ObbCollisionMode.NONE);
        if (safeMode != ObbCollisionMode.NONE) {
            temporaryCollisionModes.put(safeScope, safeMode);
        }
    }

    public synchronized ObbCollisionMode collisionMode() {
        if (!hasAttribute(ObbBoneAttribute.COLLISION)) {
            return ObbCollisionMode.NONE;
        }
        boolean persistentCollision = !disabledAttributes.contains(ObbBoneAttribute.COLLISION)
                && (registeredAttributes.contains(ObbBoneAttribute.COLLISION)
                        || persistentAttributes.contains(ObbBoneAttribute.COLLISION)
                        || persistentGroupAttributes.contains(ObbBoneAttribute.COLLISION));
        ObbCollisionMode result = persistentCollision
                ? persistentCollisionMode == null ? registeredCollisionMode : persistentCollisionMode
                : ObbCollisionMode.NONE;
        if (hasClientSnapshot && clientSnapshotAttributes.contains(ObbBoneAttribute.COLLISION)
                && !disabledAttributes.contains(ObbBoneAttribute.COLLISION)) {
            result = clientCollisionMode;
        }
        for (ObbCollisionMode mode : temporaryCollisionModes.values()) {
            result = ObbCollisionMode.strongest(result, mode);
        }
        return result;
    }

    public synchronized void setCollisionMode(ObbCollisionMode collisionMode) {
        persistentCollisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
        setAttribute(ObbBoneAttribute.COLLISION, persistentCollisionMode != ObbCollisionMode.NONE);
    }

    public synchronized <T> Optional<T> getData(ObbDataKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T typedValue = (T) value;
        return Optional.of(typedValue);
    }

    public synchronized <T> T getOrCreateData(ObbDataKey<T> key) {
        return getData(key).orElseGet(() -> {
            T value = key.createDefaultValue();
            data.put(key, value);
            return value;
        });
    }

    public synchronized <T> void setData(ObbDataKey<T> key, T value) {
        data.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    /** CN: 此骨骼是否为可承载实体的 OBB 表面。EN: Whether this bone is an entity-carrying OBB surface. */
    public boolean carriesEntities() {
        return getData(ObbBuiltinDataKeys.CARRIES_ENTITIES).orElse(false);
    }

    public void setCarriesEntities(boolean carriesEntities) {
        setData(ObbBuiltinDataKeys.CARRIES_ENTITIES, carriesEntities);
    }

    synchronized void setPersistentGroupAttribute(ObbBoneAttribute attribute, boolean enabled) {
        if (enabled) {
            persistentGroupAttributes.add(attribute);
        } else {
            persistentGroupAttributes.remove(attribute);
        }
    }

    synchronized void applyClientSnapshot(Set<ObbBoneAttribute> attributes, ObbCollisionMode collisionMode) {
        clientSnapshotAttributes.clear();
        clientSnapshotAttributes.addAll(attributes);
        clientCollisionMode = collisionMode == null ? ObbCollisionMode.NONE : collisionMode;
        hasClientSnapshot = true;
    }

    synchronized void clearClientSnapshot() {
        clientSnapshotAttributes.clear();
        clientCollisionMode = ObbCollisionMode.NONE;
        hasClientSnapshot = false;
    }

    synchronized Set<ObbBoneAttribute> persistentAttributes() {
        return persistentAttributes.isEmpty() ? Set.of() : Set.copyOf(persistentAttributes);
    }

    synchronized Set<ObbBoneAttribute> registeredAttributes() {
        return registeredAttributes.isEmpty() ? Set.of() : Set.copyOf(registeredAttributes);
    }

    synchronized Set<ObbBoneAttribute> disabledAttributes() {
        return disabledAttributes.isEmpty() ? Set.of() : Set.copyOf(disabledAttributes);
    }

    synchronized Optional<ObbCollisionMode> persistentCollisionModeOverride() {
        return Optional.ofNullable(persistentCollisionMode);
    }

    synchronized Map<ObbDataKey<?>, Object> dataEntries() {
        return Map.copyOf(data);
    }

    synchronized void loadPersistentState(Set<ObbBoneAttribute> attributes, Set<ObbBoneAttribute> disabled,
            ObbCollisionMode collisionMode) {
        persistentAttributes.clear();
        persistentAttributes.addAll(attributes);
        disabledAttributes.clear();
        disabledAttributes.addAll(disabled);
        persistentCollisionMode = collisionMode;
    }

    private static EnumSet<ObbBoneAttribute> copyAttributes(Set<ObbBoneAttribute> attributes) {
        return attributes == null || attributes.isEmpty()
                ? EnumSet.noneOf(ObbBoneAttribute.class)
                : EnumSet.copyOf(attributes);
    }

    private static String requireScope(String scope) {
        String safeScope = Objects.requireNonNull(scope, "scope").trim();
        if (safeScope.isEmpty()) {
            throw new IllegalArgumentException("Temporary OBB attribute scope must not be blank");
        }
        return safeScope;
    }
}
