package com.kltyton.bedrockruntime.client.particle.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.IComponent;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.curve.ParticleCurve;
import com.kltyton.bedrockruntime.client.particle.definition.description.ParticleDescription;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefinedParticleEffect {
    private static final Map<String, Integer> COMPONENT_SOURCE_ORDER = Map.ofEntries(
            Map.entry("minecraft:emitter_local_space", 0),
            Map.entry("minecraft:emitter_initialization", 1),
            Map.entry("minecraft:emitter_lifetime_looping", 2),
            Map.entry("minecraft:emitter_lifetime_once", 3),
            Map.entry("minecraft:emitter_lifetime_expression", 4),
            Map.entry("minecraft:emitter_lifetime_events", 5),
            Map.entry("minecraft:emitter_rate_instant", 6),
            Map.entry("minecraft:emitter_rate_steady", 7),
            Map.entry("minecraft:emitter_rate_manual", 8),
            Map.entry("minecraft:emitter_shape_point", 9),
            Map.entry("minecraft:emitter_shape_box", 10),
            Map.entry("minecraft:emitter_shape_sphere", 11),
            Map.entry("minecraft:emitter_shape_custom", 12),
            Map.entry("minecraft:emitter_shape_entity_aabb", 13),
            Map.entry("minecraft:emitter_shape_disc", 14),
            Map.entry("minecraft:particle_appearance_billboard", 15),
            Map.entry("minecraft:particle_appearance_tinting", 16),
            Map.entry("minecraft:particle_appearance_lighting", 17),
            Map.entry("minecraft:particle_initial_spin", 18),
            Map.entry("minecraft:particle_initial_speed", 19),
            Map.entry("minecraft:particle_initialization", 20),
            Map.entry("minecraft:particle_lifetime_expression", 21),
            Map.entry("minecraft:particle_expire_if_in_blocks", 22),
            Map.entry("minecraft:particle_expire_if_not_in_blocks", 23),
            Map.entry("minecraft:particle_kill_plane", 24),
            Map.entry("minecraft:particle_lifetime_events", 25),
            Map.entry("minecraft:particle_motion_dynamic", 26),
            Map.entry("minecraft:particle_motion_parametric", 27),
            Map.entry("minecraft:particle_motion_collision", 28),
            Map.entry("minecraft:particle_appearance_camera_distance_fade", 29)
    );

    public static final Codec<DefinedParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleDescription.CODEC.fieldOf("description").forGetter(effect -> effect.description),
            Codec.unboundedMap(Codec.STRING, ParticleCurve.CODEC).lenientOptionalFieldOf("curves", Map.of()).forGetter(effect -> effect.curves),
            Codec.dispatchedMap(ResourceLocation.CODEC, IComponent.COMPONENTS::get).fieldOf("components").forGetter(effect -> effect.components),
            Codec.unboundedMap(Codec.STRING, IEventNode.CODEC).lenientOptionalFieldOf("events", Map.of()).forGetter(effect -> effect.events)
    ).apply(instance, DefinedParticleEffect::new));
    public final ParticleDescription description;
    public final Map<String, ParticleCurve> curves;
    public final List<Map.Entry<String, ParticleCurve>> orderedCurves;
    public final Map<ResourceLocation, IComponent> components;
    public final Map<String, Map<String, IEventNode>> events;

    public final List<IComponent> orderedComponents;
    public final List<IParticleComponent> orderedParticleComponents;
    public final List<IParticleComponent> orderedParticleComponentsWhichRequireUpdate;
    public final List<IParticleComponent> orderedParticleEarlyComponents;
    public final List<IEmitterComponent> orderedEmitterComponents;
    private MolangVersion molangVersion = MolangVersion.LATEST;
    private @Nullable JsonElement sourceFormatVersion;
    private @Nullable String sourceTexturePath;
    private @Nullable ResourceLocation sourceTextureSprite;
    private boolean bedrockBlockNames;
    private boolean bedrockItemNames;

    public boolean usesBedrockBlockNames() {
        return bedrockBlockNames;
    }

    public DefinedParticleEffect withBedrockBlockNames(boolean bedrockBlockNames) {
        this.bedrockBlockNames = bedrockBlockNames;
        return this;
    }

    public boolean usesBedrockItemNames() {
        return bedrockItemNames;
    }

    public DefinedParticleEffect withBedrockItemNames(boolean bedrockItemNames) {
        this.bedrockItemNames = bedrockItemNames;
        return this;
    }

    public MolangVersion molangVersion() {
        return molangVersion;
    }

    public DefinedParticleEffect withMolangVersion(MolangVersion version) {
        this.molangVersion = java.util.Objects.requireNonNull(version);
        return this;
    }

    public DefinedParticleEffect withSourceMetadata(@Nullable JsonElement formatVersion,
                                                     @Nullable String texturePath,
                                                     @Nullable ResourceLocation textureSprite) {
        this.sourceFormatVersion = formatVersion == null ? null : formatVersion.deepCopy();
        this.sourceTexturePath = texturePath;
        this.sourceTextureSprite = textureSprite;
        return this;
    }

    public DefinedParticleEffect withSourceFrom(DefinedParticleEffect source) {
        return withBedrockBlockNames(source.bedrockBlockNames).withBedrockItemNames(source.bedrockItemNames)
                .withMolangVersion(source.molangVersion).withSourceMetadata(
                source.sourceFormatVersion, source.sourceTexturePath, source.sourceTextureSprite);
    }

    public @Nullable JsonElement sourceFormatVersion() {
        return sourceFormatVersion == null ? null : sourceFormatVersion.deepCopy();
    }

    public @Nullable String originalTextureForExport() {
        return sourceTextureSprite != null && sourceTextureSprite.equals(description.parameters().texture())
                ? sourceTexturePath : null;
    }

    public boolean usesTerrainAtlas() {
        return "atlas.terrain".equals(originalTextureForExport());
    }

    public DefinedParticleEffect(ParticleDescription description, Map<String, ParticleCurve> curves, Map<ResourceLocation, IComponent> components, Map<String, Map<String, IEventNode>> events) {
        this.description = description;
        this.curves = curves;
        this.orderedCurves = curves.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .sorted(Map.Entry.comparingByKey()).toList();
        this.components = components;
        this.events = events;

        this.orderedComponents = new ArrayList<>();
        this.orderedParticleComponents = new ArrayList<>();
        this.orderedParticleComponentsWhichRequireUpdate = new ArrayList<>();
        this.orderedParticleEarlyComponents = new ArrayList<>();
        this.orderedEmitterComponents = new ArrayList<>();
        components.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, IComponent> entry) -> entry.getValue().order())
                        .thenComparingInt(entry -> COMPONENT_SOURCE_ORDER.getOrDefault(entry.getKey().toString(), Integer.MAX_VALUE)))
                .map(Map.Entry::getValue).forEachOrdered(orderedComponents::add);
        for (IComponent component : orderedComponents) {
            if (component instanceof IParticleComponent particleComponent) {
                if (component.order() < 0) {
                    orderedParticleEarlyComponents.add(particleComponent);
                    continue;
                }
                orderedParticleComponents.add(particleComponent);
                if (particleComponent.requireUpdate()) {
                    orderedParticleComponentsWhichRequireUpdate.add(particleComponent);
                }
            } else if (component instanceof IEmitterComponent emitterComponent) {
                orderedEmitterComponents.add(emitterComponent);
            }
        }
    }
}
