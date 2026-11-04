package com.kltyton.bedrockruntime.client.particle.definition.description;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomMaterialEvent;
import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DescriptionMaterial implements StringRepresentable {
    private static List<DescriptionMaterial> pending = new ArrayList<>();
    private static Map<String, DescriptionMaterial> values;
    private static List<DescriptionMaterial> materialList;

    public static final DescriptionMaterial
            TERRAIN_SHEET = register("terrain_sheet", () -> (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS)),
            PARTICLE_SHEET_OPAQUE = register("particle_sheet_opaque"),
            PARTICLE_SHEET_TRANSLUCENT = register("particle_sheet_translucent"),
            PARTICLE_SHEET_LIT = register("particle_sheet_lit"),
            CUSTOM = register("custom"),
            NO_RENDER = register("no_renderer"),
            particles_alpha = register("particles_alpha"),
            particles_blend = register("particles_blend"),
            particles_add = register("particles_add"),
            particles_opaque = register("particles_opaque");

    private final String name;
    private final @Nullable Supplier<TextureAtlas> atlas;
    private @Nullable ParticleRenderType renderGroup;
    public static final Codec<DescriptionMaterial> CODEC = Codec.STRING.xmap(
            name -> registeredValues().getOrDefault(name, CUSTOM),
            DescriptionMaterial::getSerializedName
    );

    private DescriptionMaterial(String name, @Nullable Supplier<TextureAtlas> atlas) {
        this.name = name;
        this.atlas = atlas;
    }

    @ApiStatus.Internal
    public DescriptionMaterial withRenderGroup(ParticleRenderType group) {
        if (pending == null) throw new IllegalStateException("Particle materials are already frozen");
        this.renderGroup = java.util.Objects.requireNonNull(group);
        return this;
    }

    public @Nullable ParticleRenderType getRenderGroup() {
        return renderGroup;
    }

    private static DescriptionMaterial register(String name) {
        return register(name, null);
    }

    @ApiStatus.Internal
    public static DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas) {
        if (pending == null) {
            throw new IllegalStateException("Particle materials are already frozen");
        }
        DescriptionMaterial material = new DescriptionMaterial(name, atlas);
        pending.add(material);
        return material;
    }

    private static synchronized Map<String, DescriptionMaterial> registeredValues() {
        if (values == null) {
            fireRegistrationEvent();
            Map<String, DescriptionMaterial> materialMap = new HashMap<>();
            for (DescriptionMaterial material : pending) {
                materialMap.put(material.name, material);
            }
            materialMap.putIfAbsent("no_render", NO_RENDER);
            values = Map.copyOf(materialMap);
            materialList = List.copyOf(pending);
            pending = null;
        }
        return values;
    }

    private static void fireRegistrationEvent() {
        ParticleEvents.post(new RegisterCustomMaterialEvent((name, atlas) -> register(name, atlas)));
    }

    public static DescriptionMaterial[] values() {
        registeredValues();
        return materialList.toArray(DescriptionMaterial[]::new);
    }

    public TextureAtlas getAtlas() {
        if (atlas != null) {
            TextureAtlas selected = atlas.get();
            if (selected != null) return selected;
        }
        return (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
