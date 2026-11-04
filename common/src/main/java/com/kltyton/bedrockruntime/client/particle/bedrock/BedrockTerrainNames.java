package com.kltyton.bedrockruntime.client.particle.bedrock;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.Set;

/** Vanilla texture names that differ between the Java and Bedrock block atlases. */
final class BedrockTerrainNames {
    private static final Set<String> COLORS = Set.of("white", "orange", "magenta", "light_blue", "yellow",
            "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final Set<String> WOODS = Set.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak");
    private static final Map<String, List<String>> RELATED_BLOCKS = Map.ofEntries(
            Map.entry("granite", List.of("granite_stairs")),
            Map.entry("diorite", List.of("diorite_stairs")),
            Map.entry("andesite", List.of("andesite_stairs")),
            Map.entry("polished_granite", List.of("polished_granite_stairs")),
            Map.entry("polished_diorite", List.of("polished_diorite_stairs")),
            Map.entry("polished_andesite", List.of("polished_andesite_stairs")),
            Map.entry("mossy_cobblestone", List.of("mossy_cobblestone_stairs")),
            Map.entry("bricks", List.of("brick_stairs")),
            Map.entry("nether_bricks", List.of("nether_brick_stairs", "nether_brick_fence")),
            Map.entry("red_nether_bricks", List.of("red_nether_brick_stairs")),
            Map.entry("end_stone_bricks", List.of("end_brick_stairs")),
            Map.entry("sandstone_bottom", List.of("sandstone")),
            Map.entry("sandstone_top", List.of("chiseled_sandstone", "cut_sandstone", "smooth_sandstone")),
            Map.entry("red_sandstone_bottom", List.of("red_sandstone")),
            Map.entry("red_sandstone_top", List.of("chiseled_red_sandstone", "cut_red_sandstone", "smooth_red_sandstone")),
            Map.entry("basalt_top", List.of("basalt")),
            Map.entry("polished_basalt_top", List.of("polished_basalt")),
            Map.entry("deepslate_top", List.of("deepslate")),
            Map.entry("blackstone_top", List.of("blackstone")),
            Map.entry("sea_lantern", List.of("seaLantern"))
    );
    private static final Map<String, String> TILES = Map.ofEntries(
            Map.entry("stone", "flattened_stone"),
            Map.entry("sand", "flattened_sand"),
            Map.entry("dirt", "flattened_dirt"),
            Map.entry("granite", "granite"),
            Map.entry("diorite", "diorite"),
            Map.entry("andesite", "andesite"),
            Map.entry("polished_granite", "polished_granite"),
            Map.entry("polished_diorite", "polished_diorite"),
            Map.entry("polished_andesite", "polished_andesite"),
            Map.entry("stone_bricks", "stone_bricks"),
            Map.entry("mossy_stone_bricks", "mossy_stone_bricks"),
            Map.entry("cracked_stone_bricks", "cracked_stone_bricks"),
            Map.entry("chiseled_stone_bricks", "chiseled_stone_bricks"),
            Map.entry("mossy_cobblestone", "cobblestone_mossy"),
            Map.entry("bricks", "brick"),
            Map.entry("nether_bricks", "nether_brick"),
            Map.entry("red_nether_bricks", "red_nether_brick"),
            Map.entry("end_stone_bricks", "end_bricks"),
            Map.entry("smooth_stone", "smooth_stone"),
            Map.entry("terracotta", "hardened_clay"),
            Map.entry("prismarine", "flattened_prismarine"),
            Map.entry("slime_block", "slime_block"),
            Map.entry("red_sand", "red_sand"),
            Map.entry("coarse_dirt", "coarse_dirt"),
            Map.entry("sandstone_bottom", "flattened_sandstone_bottom"),
            Map.entry("sandstone_top", "flattened_sandstone_top"),
            Map.entry("red_sandstone_bottom", "flattened_redsandstone_bottom"),
            Map.entry("red_sandstone_top", "flattened_redsandstone_top"),
            Map.entry("basalt_top", "basalt_top"),
            Map.entry("polished_basalt_top", "polished_basalt_top"),
            Map.entry("deepslate_top", "deepslate_top"),
            Map.entry("blackstone_top", "blackstone_top"),
            Map.entry("sea_lantern", "sea_lantern"),
            Map.entry("packed_ice", "ice_packed"),
            Map.entry("magma", "magma"),
            Map.entry("mud", "mud"),
            Map.entry("clay", "clay"),
            Map.entry("gravel", "gravel"),
            Map.entry("smooth_basalt", "smooth_basalt"),
            Map.entry("netherrack", "netherrack"),
            Map.entry("end_stone", "end_stone"),
            Map.entry("obsidian", "obsidian"),
            Map.entry("crying_obsidian", "crying_obsidian"),
            Map.entry("bedrock", "bedrock"),
            Map.entry("cobbled_deepslate", "cobbled_deepslate"),
            Map.entry("blue_ice", "blue_ice"),
            Map.entry("ice", "ice"),
            Map.entry("snow", "snow"),
            Map.entry("calcite", "calcite"),
            Map.entry("tuff", "tuff"),
            Map.entry("dripstone_block", "dripstone_block")
    );
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("granite", "stone_granite"),
            Map.entry("diorite", "stone_diorite"),
            Map.entry("andesite", "stone_andesite"),
            Map.entry("polished_granite", "stone_granite_smooth"),
            Map.entry("polished_diorite", "stone_diorite_smooth"),
            Map.entry("polished_andesite", "stone_andesite_smooth"),
            Map.entry("stone_bricks", "stonebrick"),
            Map.entry("mossy_stone_bricks", "stonebrick_mossy"),
            Map.entry("cracked_stone_bricks", "stonebrick_cracked"),
            Map.entry("chiseled_stone_bricks", "stonebrick_carved"),
            Map.entry("mossy_cobblestone", "cobblestone_mossy"),
            Map.entry("bricks", "brick"),
            Map.entry("nether_bricks", "nether_brick"),
            Map.entry("red_nether_bricks", "red_nether_brick"),
            Map.entry("end_stone_bricks", "end_bricks"),
            Map.entry("sandstone", "sandstone_normal"),
            Map.entry("chiseled_sandstone", "sandstone_carved"),
            Map.entry("cut_sandstone", "sandstone_smooth"),
            Map.entry("red_sandstone", "red_sandstone_normal"),
            Map.entry("chiseled_red_sandstone", "red_sandstone_carved"),
            Map.entry("cut_red_sandstone", "red_sandstone_smooth"),
            Map.entry("smooth_stone", "stone_slab_top"),
            Map.entry("terracotta", "hardened_clay"),
            Map.entry("prismarine", "prismarine_rough"),
            Map.entry("slime_block", "slime"),
            Map.entry("packed_ice", "ice_packed"),
            Map.entry("oak_planks", "planks_oak"),
            Map.entry("spruce_planks", "planks_spruce"),
            Map.entry("birch_planks", "planks_birch"),
            Map.entry("jungle_planks", "planks_jungle"),
            Map.entry("acacia_planks", "planks_acacia"),
            Map.entry("dark_oak_planks", "planks_big_oak")
    );

    private BedrockTerrainNames() {}

    static @Nullable String legacyTile(String javaName) {
        return switch (javaName) {
            case "stone", "sand", "dirt" -> javaName;
            default -> null;
        };
    }

    static List<String> relatedBlocks(String javaName) {
        for (String wood : WOODS) {
            if (javaName.equals(wood + "_planks")) {
                return List.of(wood + "_slab", wood + "_double_slab", wood + "_fence");
            }
            if (javaName.equals(wood + "_log_top")) return List.of(wood + "_log");
        }
        return RELATED_BLOCKS.getOrDefault(javaName, List.of());
    }

    static @Nullable String defaultTile(String javaName) {
        String exact = TILES.get(javaName);
        if (exact != null) return exact;
        for (String wood : WOODS) {
            if (javaName.equals(wood + "_planks")) return javaName;
            if (javaName.equals(wood + "_log_top")) return javaName;
        }
        for (String color : COLORS) {
            String prefix = color + "_";
            if (!javaName.startsWith(prefix)) continue;
            String bedrockColor = color.equals("light_gray") ? "silver" : color;
            return switch (javaName.substring(prefix.length())) {
                case "wool" -> "wool_colored_" + bedrockColor;
                case "glazed_terracotta" -> bedrockColor + "_glazed_terracotta";
                case "concrete", "concrete_powder", "terracotta", "stained_glass" -> javaName;
                default -> null;
            };
        }
        return null;
    }

    static @Nullable String texture(String javaName) {
        String exact = NAMES.get(javaName);
        if (exact != null) return exact;
        for (String wood : WOODS) {
            String bedrockWood = wood.equals("dark_oak") ? "big_oak" : wood;
            if (javaName.equals(wood + "_log")) return "log_" + bedrockWood;
            if (javaName.equals(wood + "_log_top")) return "log_" + bedrockWood + "_top";
            if (javaName.equals(wood + "_leaves")) return "leaves_" + bedrockWood;
        }
        for (String color : COLORS) {
            String prefix = color + "_";
            if (!javaName.startsWith(prefix)) continue;
            String bedrockColor = color.equals("light_gray") ? "silver" : color;
            return switch (javaName.substring(prefix.length())) {
                case "concrete" -> "concrete_" + bedrockColor;
                case "concrete_powder" -> "concrete_powder_" + bedrockColor;
                case "wool" -> "wool_colored_" + bedrockColor;
                case "terracotta" -> "hardened_clay_stained_" + bedrockColor;
                case "glazed_terracotta" -> "glazed_terracotta_" + bedrockColor;
                case "stained_glass" -> "glass_" + bedrockColor;
                case "stained_glass_pane_top" -> "glass_pane_top_" + bedrockColor;
                default -> null;
            };
        }
        return null;
    }
}
