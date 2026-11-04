package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;

import java.util.Map;

/** Bedrock identifiers whose Java entity type has a different identifier. */
public final class BedrockEntityTypes {
    // ResourceLocation pairs derived from HiveGamesOSS/Chunker entity type resolvers (MIT).
    private static final Map<String, String> JAVA_NAMES = Map.ofEntries(
            Map.entry("breeze_wind_charge_projectile", "breeze_wind_charge"),
            Map.entry("ender_crystal", "end_crystal"),
            Map.entry("evocation_illager", "evoker"),
            Map.entry("evocation_fang", "evoker_fangs"),
            Map.entry("xp_bottle", "experience_bottle"),
            Map.entry("xp_orb", "experience_orb"),
            Map.entry("eye_of_ender_signal", "eye_of_ender"),
            Map.entry("fireworks_rocket", "firework_rocket"),
            Map.entry("fishing_hook", "fishing_bobber"),
            Map.entry("thrown_trident", "trident"),
            Map.entry("tropicalfish", "tropical_fish"),
            Map.entry("villager_v2", "villager"),
            Map.entry("wind_charge_projectile", "wind_charge"),
            Map.entry("zombie_villager_v2", "zombie_villager"),
            Map.entry("zombie_pigman", "zombified_piglin")
    );

    private BedrockEntityTypes() {
    }

    public static ResourceLocation identifier(Entity entity) {
        ResourceLocation actual = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!actual.getNamespace().equals("minecraft")) return actual;
        for (Map.Entry<String, String> entry : JAVA_NAMES.entrySet()) {
            if (entry.getValue().equals(actual.getPath())) return new ResourceLocation("minecraft", entry.getKey());
        }
        return actual;
    }

    public static boolean matches(ResourceLocation requested, Entity entity) {
        ResourceLocation actual = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (requested.equals(actual)) return true;
        if (!requested.getNamespace().equals("minecraft")) return false;
        if (requested.getPath().equals("boat")) {
            return entity instanceof Boat && !(entity instanceof ChestBoat);
        }
        if (requested.getPath().equals("chest_boat")) return entity instanceof ChestBoat;
        String javaName = JAVA_NAMES.get(requested.getPath());
        return javaName != null && actual.getNamespace().equals("minecraft") && actual.getPath().equals(javaName);
    }
}
