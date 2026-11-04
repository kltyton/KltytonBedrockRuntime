package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Biome names and tags used by Bedrock particle expressions in vanilla worlds.
 * Java-to-Bedrock mappings include data from GeyserMC mappings; its license is in META-INF/licenses.
 */
final class BedrockBiomeMapping {
    private static final Map<String, BedrockBiome> VANILLA = Map.ofEntries(
        Map.entry("badlands", new BedrockBiome("mesa", Set.of("animal", "mesa", "monster", "overworld", "spawns_mesa_mobs", "spawns_warm_variant_farm_animals", "surface_mineshaft"))),
        Map.entry("bamboo_jungle", new BedrockBiome("bamboo_jungle", Set.of("animal", "bamboo", "jungle", "monster", "overworld", "spawns_jungle_mobs", "spawns_warm_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("basalt_deltas", new BedrockBiome("basalt_deltas", Set.of("nether", "basalt_deltas", "spawn_many_magma_cubes", "spawn_ghast", "spawns_warm_variant_farm_animals"))),
        Map.entry("beach", new BedrockBiome("beach", Set.of("beach", "monster", "overworld", "warm"))),
        Map.entry("birch_forest", new BedrockBiome("birch_forest", Set.of("animal", "birch", "forest", "monster", "overworld", "bee_habitat", "has_structure_abandoned_camp"))),
        Map.entry("cherry_grove", new BedrockBiome("cherry_grove", Set.of("mountains", "monster", "overworld", "cherry_grove", "bee_habitat", "has_structure_abandoned_camp"))),
        Map.entry("cold_ocean", new BedrockBiome("cold_ocean", Set.of("cold", "monster", "ocean", "overworld", "spawns_cold_variant_farm_animals", "fast_fishing", "high_seas"))),
        Map.entry("crimson_forest", new BedrockBiome("crimson_forest", Set.of("nether", "netherwart_forest", "crimson_forest", "spawn_few_zombified_piglins", "spawn_piglin", "spawns_warm_variant_farm_animals"))),
        Map.entry("dappled_forest", new BedrockBiome("dappled_forest", Set.of("animal", "dappled_forest", "monster", "forest", "overworld", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("dark_forest", new BedrockBiome("roofed_forest", Set.of("animal", "forest", "monster", "no_legacy_worldgen", "overworld", "roofed"))),
        Map.entry("deep_cold_ocean", new BedrockBiome("deep_cold_ocean", Set.of("cold", "deep", "monster", "ocean", "overworld", "spawns_cold_variant_farm_animals", "fast_fishing", "high_seas"))),
        Map.entry("deep_dark", new BedrockBiome("deep_dark", Set.of("caves", "deep_dark", "overworld", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("deep_frozen_ocean", new BedrockBiome("deep_frozen_ocean", Set.of("deep", "frozen", "monster", "ocean", "overworld", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_polar_bears_on_alternate_blocks", "fast_fishing", "high_seas"))),
        Map.entry("deep_lukewarm_ocean", new BedrockBiome("deep_lukewarm_ocean", Set.of("deep", "lukewarm", "monster", "ocean", "overworld", "spawns_warm_variant_farm_animals", "fast_fishing", "high_seas", "temperate_ocean"))),
        Map.entry("deep_ocean", new BedrockBiome("deep_ocean", Set.of("deep", "monster", "ocean", "overworld", "spawns_warm_variant_farm_animals", "fast_fishing", "high_seas"))),
        Map.entry("desert", new BedrockBiome("desert", Set.of("desert", "monster", "overworld", "spawns_gold_rabbits", "spawns_warm_variant_farm_animals", "spawns_warm_variant_frogs"))),
        Map.entry("dripstone_caves", new BedrockBiome("dripstone_caves", Set.of("caves", "overworld", "dripstone_caves", "monster"))),
        Map.entry("end_barrens", new BedrockBiome("the_end", Set.of("the_end", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("end_highlands", new BedrockBiome("the_end", Set.of("the_end", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("end_midlands", new BedrockBiome("the_end", Set.of("the_end", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("eroded_badlands", new BedrockBiome("mesa_bryce", Set.of("animal", "mesa", "monster", "mutated", "overworld", "spawns_mesa_mobs", "spawns_warm_variant_farm_animals", "surface_mineshaft"))),
        Map.entry("flower_forest", new BedrockBiome("flower_forest", Set.of("animal", "flower_forest", "monster", "mutated", "overworld", "bee_habitat", "has_structure_abandoned_camp"))),
        Map.entry("forest", new BedrockBiome("forest", Set.of("animal", "forest", "monster", "overworld", "bee_habitat", "has_structure_abandoned_camp", "oak_forest"))),
        Map.entry("frozen_ocean", new BedrockBiome("frozen_ocean", Set.of("frozen", "monster", "ocean", "overworld", "spawns_polar_bears_on_alternate_blocks", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits", "fast_fishing", "high_seas"))),
        Map.entry("frozen_peaks", new BedrockBiome("frozen_peaks", Set.of("mountains", "monster", "overworld", "frozen", "frozen_peaks", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("frozen_river", new BedrockBiome("frozen_river", Set.of("frozen", "overworld", "river", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_river_mobs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("grove", new BedrockBiome("grove", Set.of("mountains", "cold", "monster", "overworld", "grove", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("ice_spikes", new BedrockBiome("ice_plains_spikes", Set.of("frozen", "ice_plains", "monster", "mutated", "overworld", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_white_rabbits"))),
        Map.entry("jagged_peaks", new BedrockBiome("jagged_peaks", Set.of("mountains", "monster", "overworld", "frozen", "jagged_peaks", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("jungle", new BedrockBiome("jungle", Set.of("animal", "has_structure_trail_ruins", "jungle", "monster", "overworld", "rare", "spawns_jungle_mobs", "spawns_warm_variant_farm_animals"))),
        Map.entry("lukewarm_ocean", new BedrockBiome("lukewarm_ocean", Set.of("lukewarm", "monster", "ocean", "overworld", "spawns_warm_variant_farm_animals", "fast_fishing", "high_seas", "temperate_ocean"))),
        Map.entry("lush_caves", new BedrockBiome("lush_caves", Set.of("caves", "lush_caves", "overworld", "monster", "spawns_tropical_fish_at_any_height"))),
        Map.entry("mangrove_swamp", new BedrockBiome("mangrove_swamp", Set.of("mangrove_swamp", "overworld", "monster", "spawns_slimes_on_surface", "spawns_warm_variant_farm_animals", "spawns_warm_variant_frogs", "slime"))),
        Map.entry("meadow", new BedrockBiome("meadow", Set.of("mountains", "monster", "overworld", "meadow", "bee_habitat", "has_structure_abandoned_camp"))),
        Map.entry("mushroom_fields", new BedrockBiome("mushroom_island", Set.of("mooshroom_island", "overworld", "spawns_without_patrols"))),
        Map.entry("nether_wastes", new BedrockBiome("hell", Set.of("nether", "nether_wastes", "spawn_endermen", "spawn_few_piglins", "spawn_ghast", "spawn_magma_cubes", "spawns_nether_mobs", "spawn_zombified_piglin", "spawns_warm_variant_farm_animals"))),
        Map.entry("ocean", new BedrockBiome("ocean", Set.of("monster", "ocean", "overworld", "fast_fishing", "high_seas"))),
        Map.entry("old_growth_birch_forest", new BedrockBiome("birch_forest_mutated", Set.of("animal", "birch", "forest", "monster", "mutated", "bee_habitat", "overworld_generation", "has_structure_trail_ruins", "has_structure_abandoned_camp"))),
        Map.entry("old_growth_pine_taiga", new BedrockBiome("mega_taiga", Set.of("animal", "forest", "mega", "monster", "overworld", "rare", "taiga", "has_structure_trail_ruins", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("old_growth_spruce_taiga", new BedrockBiome("redwood_taiga_mutated", Set.of("animal", "forest", "mega", "monster", "mutated", "overworld", "taiga", "has_structure_trail_ruins", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("pale_garden", new BedrockBiome("pale_garden", Set.of("monster", "overworld", "pale_garden", "has_structure_abandoned_camp"))),
        Map.entry("plains", new BedrockBiome("plains", Set.of("animal", "monster", "overworld", "plains", "bee_habitat"))),
        Map.entry("river", new BedrockBiome("river", Set.of("overworld", "spawns_more_frequent_drowned", "spawns_reduced_water_ambient_mobs", "spawns_river_mobs", "river"))),
        Map.entry("savanna", new BedrockBiome("savanna", Set.of("animal", "monster", "overworld", "savanna", "spawns_savanna_mobs", "spawns_warm_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("savanna_plateau", new BedrockBiome("savanna_plateau", Set.of("animal", "monster", "overworld", "plateau", "savanna", "spawns_savanna_mobs", "spawns_warm_variant_farm_animals"))),
        Map.entry("small_end_islands", new BedrockBiome("the_end", Set.of("the_end", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("snowy_beach", new BedrockBiome("cold_beach", Set.of("beach", "cold", "monster", "overworld", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("snowy_plains", new BedrockBiome("ice_plains", Set.of("frozen", "ice", "ice_plains", "overworld", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits"))),
        Map.entry("snowy_slopes", new BedrockBiome("snowy_slopes", Set.of("frozen", "mountains", "monster", "overworld", "spawns_cold_variant_frogs", "spawns_snow_foxes", "spawns_white_rabbits", "snowy_slopes", "spawns_cold_variant_farm_animals"))),
        Map.entry("snowy_taiga", new BedrockBiome("cold_taiga", Set.of("animal", "cold", "forest", "monster", "overworld", "taiga", "has_structure_trail_ruins", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("soul_sand_valley", new BedrockBiome("soulsand_valley", Set.of("nether", "soulsand_valley", "spawn_ghast", "spawn_endermen", "spawns_warm_variant_farm_animals"))),
        Map.entry("sparse_jungle", new BedrockBiome("jungle_edge", Set.of("animal", "edge", "jungle", "monster", "overworld", "spawns_jungle_mobs", "spawns_warm_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("stony_peaks", new BedrockBiome("stony_peaks", Set.of("mountains", "monster", "overworld", "spawns_cold_variant_farm_animals"))),
        Map.entry("stony_shore", new BedrockBiome("stone_beach", Set.of("beach", "monster", "overworld", "stone"))),
        Map.entry("sulfur_caves", new BedrockBiome("sulfur_caves", Set.of("caves", "sulfur_caves", "overworld", "monster"))),
        Map.entry("sunflower_plains", new BedrockBiome("sunflower_plains", Set.of("animal", "monster", "mutated", "overworld", "plains", "bee_habitat"))),
        Map.entry("swamp", new BedrockBiome("swampland", Set.of("animal", "monster", "overworld", "swamp", "spawns_slimes_on_surface", "slime", "swamp_water_huge_mushroom", "has_structure_abandoned_camp"))),
        Map.entry("taiga", new BedrockBiome("taiga", Set.of("animal", "forest", "monster", "overworld", "taiga", "has_structure_trail_ruins", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("the_end", new BedrockBiome("the_end", Set.of("the_end", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"))),
        Map.entry("warm_ocean", new BedrockBiome("warm_ocean", Set.of("monster", "ocean", "overworld", "warm", "spawns_warm_variant_farm_animals", "spawns_warm_variant_frogs", "fast_fishing", "high_seas", "temperate_ocean"))),
        Map.entry("warped_forest", new BedrockBiome("warped_forest", Set.of("nether", "netherwart_forest", "warped_forest", "spawn_endermen", "spawns_warm_variant_farm_animals"))),
        Map.entry("windswept_forest", new BedrockBiome("extreme_hills_plus_trees", Set.of("animal", "extreme_hills", "forest", "monster", "mountain", "overworld", "spawns_cold_variant_farm_animals", "has_structure_abandoned_camp"))),
        Map.entry("windswept_gravelly_hills", new BedrockBiome("extreme_hills_mutated", Set.of("animal", "extreme_hills", "monster", "mutated", "overworld", "spawns_cold_variant_farm_animals"))),
        Map.entry("windswept_hills", new BedrockBiome("extreme_hills", Set.of("animal", "extreme_hills", "monster", "overworld", "spawns_cold_variant_farm_animals"))),
        Map.entry("windswept_savanna", new BedrockBiome("savanna_mutated", Set.of("animal", "monster", "mutated", "overworld", "savanna", "spawns_savanna_mobs", "spawns_warm_variant_farm_animals"))),
        Map.entry("wooded_badlands", new BedrockBiome("mesa_plateau_stone", Set.of("animal", "mesa", "monster", "overworld", "plateau", "rare", "stone", "spawns_mesa_mobs", "spawns_warm_variant_farm_animals", "surface_mineshaft", "has_structure_abandoned_camp")))
    );

    private BedrockBiomeMapping() {}

    static BedrockBiome forJava(Identifier biome) {
        return biome.getNamespace().equals("minecraft") ? VANILLA.get(biome.getPath()) : null;
    }

    record BedrockBiome(String name, Set<String> tags) {
        boolean matchesIdentifier(Identifier identifier) {
            return identifier.getNamespace().equals("minecraft") && identifier.getPath().equals(name);
        }
    }
}

