package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderItemContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query.BedrockEntityTypes;
import com.kltyton.bedrockruntime.client.particle.definition.molang.BedrockItemIdentifiers;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Constant;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Particle-effect names declared by Bedrock entity and attachable resources. */
public final class BedrockParticleBindings {
    private static volatile Snapshot active = Snapshot.empty();

    public static final class Snapshot {
        private final Map<Identifier, Map<String, Identifier>> entities;
        private final Map<Identifier, List<Attachable>> attachables;
        private final Map<Identifier, Attachable> attachableDefaults;

        private Snapshot(Map<Identifier, Map<String, Identifier>> entities,
                         Map<Identifier, List<Attachable>> attachables,
                         Map<Identifier, Attachable> attachableDefaults) {
            this.entities = Map.copyOf(entities);
            this.attachables = Map.copyOf(attachables);
            this.attachableDefaults = Map.copyOf(attachableDefaults);
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Map.of());
        }
    }

    private record RawAttachable(Identifier id, Identifier item, boolean explicitItem, boolean bedrockItemNames,
                                 @Nullable JsonElement condition,
                                 Resource resource, Map<String, Identifier> effects) {}

    private record AttachableDefinitionKey(Identifier id, boolean bedrockMeaning) {}

    private record Attachable(@Nullable MathValue condition, @Nullable RuntimeException conditionError,
                              Map<String, Identifier> effects) {
        private boolean matches(MolangInstance context) {
            if (conditionError != null) throw conditionError;
            return java.util.Objects.requireNonNull(condition).get(context) != 0.0;
        }
    }

    public record ItemResolution(boolean selected, @Nullable Identifier particle) {}

    private BedrockParticleBindings() {}

    public static Map<Identifier, Map<String, Identifier>> load(ResourceManager resources) {
        BindingResources bindingResources = collectResources(resources, true);
        return loadEntities(bindingResources.entities(), bindingResources.topVersion());
    }

    private static Map<Identifier, Map<String, Identifier>> loadEntities(
            List<Map.Entry<Identifier, Resource>> entries, DefinitionVersion topVersion) {
        if (entries.isEmpty()) return Map.of();
        Map<Identifier, Map<String, Identifier>> bindings = new HashMap<>();
        Map<Identifier, DefinitionVersion> versions = new HashMap<>();
        for (var entry : entries) {
            try (Reader reader = entry.getValue().openAsReader()) {
                StringWriter contents = new StringWriter();
                reader.transferTo(contents);
                String text = contents.toString();
                JsonObject document;
                try {
                    document = BedrockJson.parse(new StringReader(text));
                } catch (JsonParseException exception) {
                    if (text.contains("\"minecraft:client_entity\"") && text.contains("\"particle_effects\"")) throw exception;
                    BedrockParticles.LOGGER.debug("Skipping non-particle entity resource {}", entry.getKey(), exception);
                    continue;
                }
                JsonElement entity = document.get("minecraft:client_entity");
                if (entity == null || !entity.isJsonObject()) continue;
                JsonElement description = entity.getAsJsonObject().get("description");
                if (description == null || !description.isJsonObject()) continue;
                JsonObject object = description.getAsJsonObject();
                JsonElement rawId = object.get("identifier");
                JsonElement effects = object.get("particle_effects");
                if (rawId == null || !rawId.isJsonPrimitive() || !rawId.getAsJsonPrimitive().isString()) {
                    if (effects != null && !effects.isJsonNull()) {
                        throw new IllegalArgumentException("Client entity with particle effects lacks an identifier");
                    }
                    continue;
                }
                Identifier id = Identifier.tryParse(rawId.getAsString());
                if (id == null) {
                    if (effects != null && !effects.isJsonNull()) {
                        throw new IllegalArgumentException("Invalid client entity identifier: " + rawId);
                    }
                    continue;
                }
                DefinitionVersion version = definitionVersion(object.get("min_engine_version"), entry.getKey());
                DefinitionVersion previous = versions.get(id);
                if (version.compareTo(topVersion) > 0 || previous != null && version.compareTo(previous) <= 0) continue;
                Map<String, Identifier> names = new HashMap<>();
                if (effects != null && !effects.isJsonNull()) {
                    if (!effects.isJsonObject()) throw new IllegalArgumentException("Invalid particle effects for " + id);
                    for (var effect : effects.getAsJsonObject().entrySet()) {
                        JsonElement value = effect.getValue();
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                            throw new IllegalArgumentException("Invalid particle effect " + effect.getKey() + " for " + id);
                        }
                        Identifier particle = Identifier.tryParse(value.getAsString());
                        if (particle == null) throw new IllegalArgumentException("Invalid particle identifier for " + id);
                        names.put(effect.getKey(), particle);
                    }
                }
                bindings.put(id, Map.copyOf(names));
                versions.put(id, version);
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Cannot read particle bindings from " + entry.getKey(), exception);
            }
        }
        return Map.copyOf(bindings);
    }

    public static void publish(Map<Identifier, Map<String, Identifier>> bindings) {
        Snapshot current = active;
        active = new Snapshot(bindings, current.attachables, current.attachableDefaults);
    }

    public static Snapshot loadAll(ResourceManager resources) {
        BindingResources bindingResources = collectResources(resources, false);
        Map<Identifier, Map<String, Identifier>> entities = loadEntities(
                bindingResources.entities(), bindingResources.topVersion());
        List<Map.Entry<Identifier, Resource>> entries = bindingResources.attachables();
        if (entries.isEmpty()) return new Snapshot(entities, Map.of(), Map.of());
        DefinitionVersion topVersion = bindingResources.topVersion();

        Map<AttachableDefinitionKey, RawAttachable> definitions = new LinkedHashMap<>();
        Map<AttachableDefinitionKey, DefinitionVersion> versions = new HashMap<>();
        for (var entry : entries) {
            try (Reader reader = entry.getValue().openAsReader()) {
                StringWriter contents = new StringWriter();
                reader.transferTo(contents);
                String text = contents.toString();
                JsonObject document;
                try {
                    document = BedrockJson.parse(new StringReader(text));
                } catch (JsonParseException exception) {
                    if (text.contains("\"minecraft:attachable\"") && text.contains("\"particle_effects\"")) throw exception;
                    BedrockParticles.LOGGER.debug("Skipping non-particle attachable resource {}", entry.getKey(), exception);
                    continue;
                }
                JsonElement attachable = document.get("minecraft:attachable");
                if (attachable == null || !attachable.isJsonObject()) continue;
                JsonElement description = attachable.getAsJsonObject().get("description");
                if (description == null || !description.isJsonObject()) continue;
                JsonObject object = description.getAsJsonObject();
                JsonElement rawId = object.get("identifier");
                JsonElement effects = object.get("particle_effects");
                boolean hasEffects = effects != null && !effects.isJsonNull();
                if (rawId == null || !rawId.isJsonPrimitive() || !rawId.getAsJsonPrimitive().isString()) {
                    if (hasEffects) throw new IllegalArgumentException("Attachable with particle effects lacks an identifier");
                    continue;
                }
                Identifier id = Identifier.tryParse(rawId.getAsString());
                if (id == null) {
                    if (hasEffects) throw new IllegalArgumentException("Invalid attachable identifier: " + rawId);
                    continue;
                }
                boolean bedrockSource = BedrockParticleResources.usesBedrockItemNames(document, true);
                AttachableDefinitionKey key = new AttachableDefinitionKey(id,
                        bedrockSource && BedrockItemIdentifiers.hasAmbiguousBedrockMeaning(id));
                DefinitionVersion version = definitionVersion(object.get("min_engine_version"), entry.getKey());
                DefinitionVersion previous = versions.get(key);
                if (version.compareTo(topVersion) > 0 || previous != null && version.compareTo(previous) <= 0) continue;
                versions.put(key, version);
                definitions.remove(key);
                Identifier item = id;
                boolean explicitItem = false;
                JsonElement condition = null;
                JsonElement rawItem = object.get("item");
                if (rawItem != null && !rawItem.isJsonNull()) {
                    if (rawItem.isJsonPrimitive() && rawItem.getAsJsonPrimitive().isString()) {
                        item = Identifier.tryParse(rawItem.getAsString());
                        explicitItem = item != null;
                    } else if (rawItem.isJsonObject() && !rawItem.getAsJsonObject().entrySet().isEmpty()) {
                        var first = rawItem.getAsJsonObject().entrySet().iterator().next();
                        item = Identifier.tryParse(first.getKey());
                        condition = first.getValue();
                        explicitItem = item != null;
                    }
                    if (!explicitItem) {
                        BedrockParticles.LOGGER.warn("Invalid item binding for attachable {} in {}", id, entry.getKey());
                        item = id;
                        condition = null;
                    }
                }
                Map<String, Identifier> names = new HashMap<>();
                if (hasEffects) {
                    if (!effects.isJsonObject()) throw new IllegalArgumentException("Invalid particle effects for " + id);
                    for (var effect : effects.getAsJsonObject().entrySet()) {
                        JsonElement value = effect.getValue();
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                            throw new IllegalArgumentException("Invalid particle effect " + effect.getKey() + " for " + id);
                        }
                        Identifier particle = Identifier.tryParse(value.getAsString());
                        if (particle == null) throw new IllegalArgumentException("Invalid particle identifier for " + id);
                        names.put(effect.getKey(), particle);
                    }
                }
                definitions.put(key, new RawAttachable(id, item, explicitItem, bedrockSource,
                        condition, entry.getValue(), Map.copyOf(names)));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Cannot read attachable particle bindings from " + entry.getKey(), exception);
            }
        }

        Map<Identifier, List<RawAttachable>> grouped = new LinkedHashMap<>();
        Map<Identifier, RawAttachable> rawDefaults = new LinkedHashMap<>();
        definitions.values().forEach(definition -> {
            boolean bedrockSource = definition.bedrockItemNames();
            if (definition.explicitItem()) {
                List<Identifier> aliases = BedrockItemIdentifiers.javaAliases(definition.item(), bedrockSource);
                if (!bedrockSource || aliases.isEmpty()
                        && !BedrockItemIdentifiers.hasAmbiguousBedrockMeaning(definition.item())) {
                    grouped.computeIfAbsent(definition.item(), ignored -> new ArrayList<>()).add(definition);
                }
                for (Identifier alias : aliases) grouped.computeIfAbsent(alias, ignored -> new ArrayList<>()).add(definition);
            }
            List<Identifier> defaultAliases = BedrockItemIdentifiers.javaAliases(definition.id(), bedrockSource);
            if (!bedrockSource || defaultAliases.isEmpty()
                    && !BedrockItemIdentifiers.hasAmbiguousBedrockMeaning(definition.id())) {
                rawDefaults.putIfAbsent(definition.id(), definition);
            }
            for (Identifier alias : defaultAliases) rawDefaults.putIfAbsent(alias, definition);
        });
        Map<Identifier, List<Attachable>> bindings = new HashMap<>();
        Map<Identifier, Attachable> defaults = new HashMap<>();
        Map<RawAttachable, Attachable> compiled = new java.util.IdentityHashMap<>();
        Map<String, MolangVersion> packVersions = new HashMap<>();
        var items = new java.util.LinkedHashSet<>(grouped.keySet());
        items.addAll(rawDefaults.keySet());
        for (Identifier item : items) {
            List<RawAttachable> group = grouped.getOrDefault(item, List.of());
            RawAttachable fallback = rawDefaults.get(item);
            boolean hasParticleEffects = group.stream().anyMatch(definition -> !definition.effects().isEmpty())
                    || fallback != null && !fallback.effects().isEmpty();
            if (fallback != null) defaults.put(item, new Attachable(new Constant(1.0), null, fallback.effects()));
            List<Attachable> candidates = new ArrayList<>();
            for (RawAttachable definition : group) {
                try {
                    Attachable cached = compiled.get(definition);
                    if (cached != null) {
                        if (hasParticleEffects && cached.conditionError() != null) throw cached.conditionError();
                        candidates.add(cached);
                        continue;
                    }
                    MathValue condition;
                    JsonElement expression = definition.condition();
                    if (expression == null) {
                        condition = new Constant(1.0);
                    } else {
                        if (!expression.isJsonPrimitive()) throw new IllegalArgumentException("Invalid item condition");
                        var scalar = expression.getAsJsonPrimitive();
                        String text = scalar.isBoolean() ? (scalar.getAsBoolean() ? "1" : "0") : scalar.getAsString();
                        String packId = definition.resource().sourcePackId();
                        MolangVersion version = packVersions.get(packId);
                        if (version == null) {
                            version = BedrockParticleResources.readVersion(definition.resource());
                            packVersions.put(packId, version);
                        }
                        condition = new MolangParser(new VariableTable(null), version,
                                definition.bedrockItemNames()).compileMolang(text);
                    }
                    Attachable candidate = new Attachable(condition, null, definition.effects());
                    compiled.put(definition, candidate);
                    candidates.add(candidate);
                } catch (IOException | RuntimeException exception) {
                    IllegalStateException failure = new IllegalStateException(
                            "Cannot compile attachable condition for " + definition.id(), exception);
                    if (hasParticleEffects) throw failure;
                    BedrockParticles.LOGGER.warn("Cannot compile attachable condition for {}", definition.id(), exception);
                    Attachable candidate = new Attachable(null, failure, definition.effects());
                    compiled.put(definition, candidate);
                    candidates.add(candidate);
                }
            }
            if (!candidates.isEmpty()) bindings.put(item, List.copyOf(candidates));
        }
        return new Snapshot(entities, bindings, defaults);
    }

    public static void publish(Snapshot bindings) {
        active = bindings;
    }

    public static @Nullable Identifier resolve(Entity entity, String name) {
        Map<Identifier, Map<String, Identifier>> bindings = active.entities;
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Map<String, Identifier> exact = bindings.get(type);
        if (exact != null) return exact.get(name);
        Identifier result = null;
        for (var entry : bindings.entrySet()) {
            if (!BedrockEntityTypes.matches(entry.getKey(), entity)) continue;
            Identifier candidate = entry.getValue().get(name);
            if (candidate == null) continue;
            if (result != null && !result.equals(candidate)) return null;
            result = candidate;
        }
        return result;
    }

    public static ItemResolution resolve(ItemStack item, String name, Level level, Vec3 position,
                                         VariableTable variables, @Nullable ParticleEmitterAttachable attached) {
        if (item.isEmpty()) return new ItemResolution(false, null);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        Snapshot snapshot = active;
        boolean inRenderScope = RenderItemContext.currentItem() == item;
        if (inRenderScope) {
            var cached = RenderItemContext.cachedParticleBinding(snapshot, level, variables, attached);
            if (cached != null) return new ItemResolution(cached.selected(), cached.effects().get(name));
        }
        List<Attachable> candidates = snapshot.attachables.getOrDefault(itemId, List.of());
        Attachable selected = snapshot.attachableDefaults.get(itemId);
        if (!candidates.isEmpty()) {
            MolangInstance context = new AttachableContext(itemId, level, position, variables, attached);
            for (int index = candidates.size() - 1; index >= 0; index--) {
                Attachable candidate = candidates.get(index);
                if (candidate.matches(context)) selected = candidate;
            }
        }
        var result = new RenderItemContext.ParticleBindingSelection(selected != null,
                selected == null ? Map.of() : selected.effects());
        if (inRenderScope) RenderItemContext.cacheParticleBinding(snapshot, level, variables, attached, result);
        return new ItemResolution(result.selected(), result.effects().get(name));
    }

    private record AttachableContext(Identifier identity, Level level, Vec3 position, VariableTable variables,
                                     @Nullable ParticleEmitterAttachable attached) implements MolangInstance {
        @Override public VariableTable getVars() { return variables; }
        @Override public @Nullable Boolean getFirstPersonPerspective() {
            var display = RenderItemContext.currentDisplay();
            return display == null ? null : display.firstPerson();
        }
        @Override public Level getLevel() { return level; }
        @Override public float tickAge() { return 0.0F; }
        @Override public float tickLifetime() { return 0.0F; }
        @Override public double getRandom1() { return 0.0; }
        @Override public double getRandom2() { return 0.0; }
        @Override public double getRandom3() { return 0.0; }
        @Override public double getRandom4() { return 0.0; }
        @Override public Identifier getIdentity() { return identity; }
        @Override public Vec3 getPosition() { return position; }
        @Override public @Nullable Entity getAttachedEntity() { return attached instanceof Entity entity ? entity : null; }
        @Override public @Nullable ParticleEmitterAttachable getAttached() { return attached; }
        @Override public float getInvTickRate() { return 1.0F / level.tickRateManager().tickrate(); }
        @Override public @Nullable ParticleEmitter getEmitter() { return null; }
    }

    private record BindingResources(List<Map.Entry<Identifier, Resource>> entities,
                                    List<Map.Entry<Identifier, Resource>> attachables,
                                    DefinitionVersion topVersion) {}

    private static BindingResources collectResources(ResourceManager resources, boolean entitiesOnly) {
        List<PackResources> packs = resources.listPacks().toList();
        Map<String, Integer> priorities = new HashMap<>();
        for (int index = 0; index < packs.size(); index++) priorities.put(packs.get(index).packId(), index);
        var entities = listBindingResources(resources, BedrockPackResources.PARTICLE_BINDINGS);
        entities.addAll(resources.listResources("entity", id -> id.getPath().endsWith(".json")).entrySet());
        if (entitiesOnly && entities.isEmpty()) return new BindingResources(entities, List.of(), DefinitionVersion.LATEST);
        var attachables = listBindingResources(resources, BedrockPackResources.ATTACHABLE_PARTICLE_BINDINGS);
        attachables.addAll(resources.listResources("attachables", id -> id.getPath().endsWith(".json")).entrySet());
        Comparator<Map.Entry<Identifier, Resource>> order = Comparator.<Map.Entry<Identifier, Resource>>comparingInt(
                entry -> priorities.getOrDefault(entry.getValue().sourcePackId(), -1)).reversed()
                .thenComparing(entry -> entry.getKey().toString());
        entities.sort(order);
        attachables.sort(order);
        DefinitionVersion version = entities.isEmpty() && attachables.isEmpty() ? DefinitionVersion.LATEST
                : topVersion(packs, entities, attachables);
        return new BindingResources(entities, attachables, version);
    }

    private static List<Map.Entry<Identifier, Resource>> listBindingResources(ResourceManager resources, String directory) {
        List<Map.Entry<Identifier, Resource>> entries = new ArrayList<>();
        resources.listResourceStacks(directory, id -> id.getPath().endsWith(".json"))
                .forEach((id, stack) -> stack.forEach(resource -> entries.add(Map.entry(id, resource))));
        return entries;
    }

    private static DefinitionVersion topVersion(List<PackResources> packs,
            List<Map.Entry<Identifier, Resource>> entities, List<Map.Entry<Identifier, Resource>> attachables) {
        Set<String> contributing = java.util.stream.Stream.concat(entities.stream(), attachables.stream())
                .map(entry -> entry.getValue().sourcePackId())
                .collect(java.util.stream.Collectors.toSet());
        for (int index = packs.size() - 1; index >= 0; index--) {
            PackResources pack = packs.get(index);
            if (!(pack instanceof BedrockPackResources) && !contributing.contains(pack.packId())) continue;
            var source = pack.getRootResource("manifest.json");
            if (source == null) continue;
            try (var input = source.get(); var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonObject document = BedrockJson.parse(reader);
                JsonElement header = document.get("header");
                if (header == null || !header.isJsonObject() || !header.getAsJsonObject().has("min_engine_version")) {
                    if (pack instanceof BedrockPackResources)
                        throw new IllegalArgumentException("Resource pack manifest lacks header.min_engine_version");
                    return DefinitionVersion.LATEST;
                }
                return DefinitionVersion.parse(header.getAsJsonObject().get("min_engine_version"));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Cannot read resource pack version for " + pack.packId(), exception);
            }
        }
        return DefinitionVersion.LATEST;
    }

    private static DefinitionVersion definitionVersion(@Nullable JsonElement value, Identifier resource) {
        if (value == null || value.isJsonNull()) return DefinitionVersion.ZERO;
        try {
            return DefinitionVersion.parse(value);
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.warn("Invalid min_engine_version in particle binding {}", resource, exception);
            return DefinitionVersion.ZERO;
        }
    }

    private record DefinitionVersion(int major, int minor, int patch, @Nullable String prerelease)
            implements Comparable<DefinitionVersion> {
        private static final DefinitionVersion ZERO = new DefinitionVersion(0, 0, 0, null);
        private static final DefinitionVersion LATEST = new DefinitionVersion(Integer.MAX_VALUE, 0, 0, null);
        private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile(
                "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");

        private static DefinitionVersion parse(JsonElement value) {
            if (value.isJsonArray()) {
                var parts = value.getAsJsonArray();
                if (parts.size() != 3) throw new IllegalArgumentException("Version must contain three components");
                int major = parts.get(0).getAsBigDecimal().intValueExact();
                int minor = parts.get(1).getAsBigDecimal().intValueExact();
                int patch = parts.get(2).getAsBigDecimal().intValueExact();
                if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("Negative version component");
                return new DefinitionVersion(major, minor, patch, null);
            }
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Version must be a string or array");
            var match = VERSION.matcher(value.getAsString());
            if (!match.matches()) throw new IllegalArgumentException("Invalid version: " + value);
            return new DefinitionVersion(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3)), match.group(4));
        }

        @Override public int compareTo(DefinitionVersion other) {
            int result = Integer.compare(major, other.major);
            if (result == 0) result = Integer.compare(minor, other.minor);
            if (result == 0) result = Integer.compare(patch, other.patch);
            if (result != 0) return result;
            if (prerelease == null) return other.prerelease == null ? 0 : 1;
            if (other.prerelease == null) return -1;
            String[] left = prerelease.split("\\.", -1);
            String[] right = other.prerelease.split("\\.", -1);
            for (int index = 0; index < Math.min(left.length, right.length); index++) {
                java.math.BigInteger leftNumber = numericIdentifier(left[index]);
                java.math.BigInteger rightNumber = numericIdentifier(right[index]);
                if (leftNumber != null && rightNumber != null) {
                    result = leftNumber.compareTo(rightNumber);
                } else if ((leftNumber != null) != (rightNumber != null)) {
                    result = leftNumber != null ? -1 : 1;
                } else {
                    result = left[index].compareTo(right[index]);
                }
                if (result != 0) return result;
            }
            return Integer.compare(left.length, right.length);
        }

        private static @Nullable java.math.BigInteger numericIdentifier(String text) {
            if (!text.matches("[0-9]+")) return null;
            var number = new java.math.BigInteger(text);
            return number.bitLength() <= 64 ? number : null;
        }
    }
}
