package com.kltyton.bedrockruntime.api.registration.selector;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;

/**
 * CN: KltytonBedrockRuntime 内置骨骼选择器。
 * EN: Built-in KltytonBedrockRuntime bone selectors.
 */
public final class ObbBoneSelectors {
    private static final Set<String> BASE_NAMES = Set.of(
            "head", "body", "torso",
            "left_arm", "leftarm", "right_arm", "rightarm",
            "left_leg", "leftleg", "right_leg", "rightleg");
    private static final Set<String> HUMAN_HELD_ITEM_NAMES = Set.of(
            "left_item", "right_item",
            "left_hand_item", "right_hand_item",
            "left_held_item", "right_held_item",
            "main_hand_item", "off_hand_item");
    private static final Set<String> HUMAN_HELD_ITEM_SOURCES = Set.of(
            "held_item", "helditem",
            "left_held_item", "right_held_item");

    /** CN: 注册模型中的所有可见 cube。EN: Registers every visible cube in the model. */
    public static final ObbBoneSelector ALL = key -> true;

    /**
     * CN: 注册头、身体、左右手臂和左右腿；同时兼容下划线与驼峰式常见名称。
     * EN: Registers head, body, both arms, and both legs, accepting common underscore and camel-case names.
     */
    public static final ObbBoneSelector BASE = key -> BASE_NAMES.contains(normalizeLeaf(key.name()));

    /**
     * CN: 人形实体预设：BASE 加左右手持物品。它只是选择器，不会自动应用到玩家或任何实体。
     * EN: Humanoid preset: BASE plus both held items. This is only a selector and is never applied automatically.
     */
    public static final ObbBoneSelector HUMAN_BASE = key -> BASE.matches(key) || isHumanHeldItem(key);

    private ObbBoneSelectors() {
    }

    public static ObbBoneSelector named(String boneName) {
        String expected = normalize(boneName);
        return key -> normalize(key.name()).equals(expected) || normalizeLeaf(key.name()).equals(expected);
    }

    public static ObbBoneSelector named(String source, String boneName) {
        String expectedSource = source == null ? "" : source;
        ObbBoneSelector nameSelector = named(boneName);
        return key -> key.source().equals(expectedSource) && nameSelector.matches(key);
    }

    /** CN: 按名称选择任意一块或多块骨骼。EN: Selects any one or more bones by name. */
    public static ObbBoneSelector namedAny(Collection<String> boneNames) {
        Set<String> expected = new LinkedHashSet<>();
        if (boneNames != null) {
            boneNames.stream()
                    .map(ObbBoneSelectors::normalize)
                    .filter(name -> !name.isEmpty())
                    .forEach(expected::add);
        }
        if (expected.isEmpty()) {
            throw new IllegalArgumentException("At least one OBB bone name is required");
        }
        return key -> expected.contains(normalize(key.name())) || expected.contains(normalizeLeaf(key.name()));
    }

    public static ObbBoneSelector exact(ObbBoneKey expected) {
        return expected::equals;
    }

    private static boolean isHumanHeldItem(ObbBoneKey key) {
        String source = normalize(key.source());
        String name = normalizeLeaf(key.name());
        return HUMAN_HELD_ITEM_SOURCES.contains(source) || HUMAN_HELD_ITEM_NAMES.contains(name);
    }

    private static String normalizeLeaf(String value) {
        String normalized = normalize(value).replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }
}
