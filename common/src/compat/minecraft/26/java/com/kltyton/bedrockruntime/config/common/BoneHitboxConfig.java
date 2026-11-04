package com.kltyton.bedrockruntime.config.common;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CN: loader 无关且 common-safe 的 KltytonBedrockRuntime 配置定义。
 * EN: Loader-neutral and common-safe KltytonBedrockRuntime config definitions.
 */
public final class BoneHitboxConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.BooleanValue VANILLA_SLANTED_BLOCK_OBB;
    private static final ModConfigSpec.BooleanValue FORCE_ALL_BLOCK_OBB;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_MODEL_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_MODEL_BLACKLIST;

    private static final ModConfigSpec.BooleanValue SHOW_CROSSHAIR_OBB;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("client");
        SHOW_CROSSHAIR_OBB = builder
                .comment(
                        "CN: 是否默认显示准星指向部位的 OBB。按住绑定键位时仍会临时显示。",
                        "EN: Whether to show the crosshair-targeted part OBB by default. Holding the bound key still shows it temporarily.")
                .define("showCrosshairObb", false);
        builder.pop();
        CLIENT_SPEC = builder.build();
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        VANILLA_SLANTED_BLOCK_OBB = common.comment(
                        "CN: 对原版斜面方块使用已打包模型的 OBB 几何。此项为服务端配置，会同步到客户端。",
                        "EN: Use packaged model OBB geometry for vanilla slanted blocks. Server config; synchronized to clients.")
                .define("vanillaSlantedBlockObb", true);
        FORCE_ALL_BLOCK_OBB = common.comment(
                        "CN: 强制将所有允许的方块碰撞转换为 OBB，优先使用已打包模型；无模型时保留方块自身的几何。空碰撞仍为空。",
                        "EN: Force OBB collision for every allowed block. Prefer packaged model geometry; otherwise retain the block's own geometry. Empty collision stays empty.")
                .define("forceAllBlockObb", false);
        BLOCK_MODEL_WHITELIST = common.comment(
                        "CN: 全局模型几何/强制 OBB 白名单。空列表不限制；支持 namespace:id、namespace:* 或 *。",
                        "EN: Global model geometry/forced OBB allowlist. Empty allows all. Entries: namespace:id, namespace:*, or *.")
                .defineListAllowEmpty("blockModelWhitelist", List.of(), () -> "minecraft:stone", BoneHitboxConfig::validBlockFilter);
        BLOCK_MODEL_BLACKLIST = common.comment(
                        "CN: 全局模型几何/强制 OBB 黑名单，优先于白名单；被排除的本库模型 OBB 回退为 AABB，其他原生形状保持原样。",
                        "EN: Global model geometry/forced OBB denylist. Overrides the allowlist; excluded library model OBBs use AABBs; other native shapes remain unchanged.")
                .defineListAllowEmpty("blockModelBlacklist", List.of(), () -> "minecraft:stone", BoneHitboxConfig::validBlockFilter);
        SERVER_SPEC = common.build();
    }

    private BoneHitboxConfig() {
    }

    public static boolean vanillaSlantedBlockObb() {
        try { return VANILLA_SLANTED_BLOCK_OBB.getAsBoolean(); }
        catch (IllegalStateException exception) { return VANILLA_SLANTED_BLOCK_OBB.getDefault(); }
    }

    public static boolean forceAllBlockObb() {
        try { return FORCE_ALL_BLOCK_OBB.getAsBoolean(); }
        catch (IllegalStateException exception) { return FORCE_ALL_BLOCK_OBB.getDefault(); }
    }

    public static boolean allowsBlockModel(Identifier blockId) {
        List<? extends String> whitelist = values(BLOCK_MODEL_WHITELIST);
        List<? extends String> blacklist = values(BLOCK_MODEL_BLACKLIST);
        if (whitelist.isEmpty() && blacklist.isEmpty()) { return true; }
        String id = blockId.toString();
        String namespace = blockId.getNamespace() + ":*";
        return !matches(blacklist, id, namespace)
                && (whitelist.isEmpty() || matches(whitelist, id, namespace));
    }

    private static boolean matches(List<? extends String> entries, String id, String namespace) {
        return entries.contains(id) || entries.contains(namespace) || entries.contains("*");
    }

    private static boolean validBlockFilter(Object value) {
        if (!(value instanceof String entry)) { return false; }
        if (entry.equals("*")) { return true; }
        int separator = entry.indexOf(':');
        if (separator <= 0 || separator == entry.length() - 1) { return false; }
        String id = entry.endsWith(":*") ? entry.substring(0, entry.length() - 1) + "block" : entry;
        return Identifier.tryParse(id) != null;
    }

    private static List<? extends String> values(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        try { return value.get(); }
        catch (IllegalStateException exception) { return value.getDefault(); }
    }

    public static boolean showCrosshairObb() {
        try {
            return SHOW_CROSSHAIR_OBB.getAsBoolean();
        } catch (IllegalStateException exception) {
            return SHOW_CROSSHAIR_OBB.getDefault();
        }
    }
}
