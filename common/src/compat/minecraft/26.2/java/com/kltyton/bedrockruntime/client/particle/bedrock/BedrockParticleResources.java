package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedrockParticleResources {
    private BedrockParticleResources() {
    }

    public record Texture(Identifier sprite, Resource resource, String sourcePath) {
    }

    public static boolean usesBedrockItemNames(JsonObject document, boolean defaultValue) {
        return usesBedrockItemNames(document.get(BedrockParticles.MODID + ":item_name_format"), defaultValue);
    }

    public static boolean usesBedrockItemNames(@Nullable JsonElement value, boolean defaultValue) {
        if (value == null) return defaultValue;
        String format = value.getAsString();
        return switch (format) {
            case "bedrock" -> true;
            case "java" -> false;
            default -> throw new com.google.gson.JsonParseException("Invalid particle item name format: " + format);
        };
    }

    public static Optional<Resource> findBedrockDefinition(ResourceManager resources, Identifier id) throws IOException {
        List<Resource> stack = resources.getResourceStack(id);
        for (int i = stack.size() - 1; i >= 0; i--) {
            Resource resource = stack.get(i);
            try {
                if (JavaParticleResourceManager.isBedrockDefinition(resource)) return Optional.of(resource);
            } catch (com.google.gson.JsonParseException exception) {
                BedrockParticles.LOGGER.error("Cannot classify particle resource {} from {}", id, resource.sourcePackId(), exception);
            }
        }
        return Optional.empty();
    }

    public static MolangVersion readVersion(Resource resource) throws IOException {
        IoSupplier<InputStream> manifest = resource.source().getRootResource("manifest.json");
        if (manifest == null) return MolangVersion.LATEST;
        try (InputStream input = manifest.get(); InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject header = object(BedrockJson.parse(reader), "header");
            if (header == null || !header.has("min_engine_version")) {
                if (resource.source() instanceof BedrockPackResources)
                    throw new IllegalArgumentException("Resource pack manifest lacks header.min_engine_version");
                return MolangVersion.LATEST;
            }
            JsonElement version = header.get("min_engine_version");
            if (version.isJsonArray()) {
                JsonArray parts = version.getAsJsonArray();
                if (parts.size() != 3) throw new IllegalArgumentException("min_engine_version must contain three components");
                return new MolangVersion(versionPart(parts.get(0)), versionPart(parts.get(1)), versionPart(parts.get(2)));
            }
            if (version.isJsonPrimitive() && version.getAsJsonPrimitive().isString()) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$")
                        .matcher(version.getAsString());
                if (matcher.matches()) return new MolangVersion(Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), matcher.group(4) != null);
            }
            throw new IllegalArgumentException("Invalid resource pack min_engine_version");
        }
    }

    private static int versionPart(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("min_engine_version components must be integers");
        return value.getAsBigDecimal().intValueExact();
    }

    public static @Nullable String sourceTexturePath(JsonObject effect) {
        JsonObject parameters = object(object(effect, "description"), "basic_render_parameters");
        if (parameters == null || !parameters.has("texture") || !parameters.get("texture").isJsonPrimitive()) return null;
        return parameters.get("texture").getAsString();
    }

    public static @Nullable Texture resolveTexture(ResourceManager resources, Identifier definition, JsonObject effect) {
        String original = sourceTexturePath(effect);
        if (original == null || original.equals("atlas.terrain")) return null;
        if (original.isBlank() || original.indexOf('\\') >= 0) return null;
        boolean bedrockPack = definition.getNamespace().equals(BedrockPackResources.NAMESPACE)
                && definition.getPath().startsWith(BedrockPackResources.DEFINITIONS + "/");
        if (!bedrockPack && original.contains(":") && !original.endsWith(".png")
                && !original.endsWith(".tga") && !original.endsWith(".jpg") && !original.endsWith(".jpeg")) return null;

        String namespace = definition.getNamespace();
        String path = original;
        int separator = original.indexOf(':');
        if (!bedrockPack && separator >= 0) {
            namespace = original.substring(0, separator);
            path = original.substring(separator + 1);
        }
        if (path.startsWith("/")) return null;
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return null;
        }
        List<String> bases = new ArrayList<>();
        if (bedrockPack) {
            bases.add(path.startsWith("textures/") ? path : "textures/" + path);
        } else {
            if (path.startsWith("textures/")) {
                bases.add(path);
            } else {
                int slash = definition.getPath().lastIndexOf('/');
                if (slash >= 0) bases.add(definition.getPath().substring(0, slash + 1) + path);
                bases.add("textures/particle/" + path);
                bases.add("textures/" + path);
            }
        }
        Map<String, Integer> priorities = new HashMap<>();
        int priority = 0;
        for (var pack : resources.listPacks().toList()) priorities.put(pack.packId(), priority++);
        for (String base : bases) {
            Texture selected = null;
            int selectedPriority = Integer.MIN_VALUE;
            for (String candidate : withExtensions(base)) {
                Identifier resourceId = bedrockPack
                        ? BedrockPackResources.textureResource(candidate)
                        : Identifier.tryBuild(namespace, candidate);
                if (resourceId == null) continue;
                Optional<Resource> resource = resources.getResource(resourceId);
                if (resource.isEmpty()) continue;
                int candidatePriority = priorities.getOrDefault(resource.get().sourcePackId(), -1);
                if (selected != null && candidatePriority <= selectedPriority) continue;
                Identifier sprite = bedrockPack
                        ? BedrockPackResources.textureSprite(candidate)
                        : Identifier.fromNamespaceAndPath(namespace, "bedrock_compat/" +
                                HexFormat.of().formatHex(candidate.getBytes(StandardCharsets.UTF_8)));
                selected = new Texture(sprite, resource.get(), original);
                selectedPriority = candidatePriority;
            }
            if (selected != null) return selected;
        }
        return null;
    }

    public static void applyTexture(JsonObject effect, Texture texture) {
        JsonObject parameters = object(object(effect, "description"), "basic_render_parameters");
        if (parameters != null) parameters.addProperty("texture", texture.sprite().toString());
    }

    static List<Identifier> spriteAliases(Texture texture) {
        if (withExtensions(texture.sourcePath()).size() == 1) return List.of();
        String path = BedrockPackResources.texturePathForSprite(texture.sprite());
        if (path == null) return List.of(texture.sprite());
        if (!texture.sprite().getNamespace().equals(BedrockPackResources.NAMESPACE)) {
            path = path.substring(path.indexOf(':') + 1);
        }
        int extension = path.lastIndexOf('.');
        if (extension < 0) return List.of(texture.sprite());
        String base = path.substring(0, extension);
        List<Identifier> aliases = new ArrayList<>();
        for (String candidate : withExtensions(base)) {
            aliases.add(texture.sprite().getNamespace().equals(BedrockPackResources.NAMESPACE)
                    ? BedrockPackResources.textureSprite(candidate)
                    : Identifier.fromNamespaceAndPath(texture.sprite().getNamespace(), "bedrock_compat/" +
                            HexFormat.of().formatHex(candidate.getBytes(StandardCharsets.UTF_8))));
        }
        return aliases;
    }

    static boolean resolvesExtensions(@Nullable String sourcePath) {
        return sourcePath != null && withExtensions(sourcePath).size() > 1;
    }

    private static @Nullable JsonObject object(@Nullable JsonObject parent, String name) {
        if (parent == null) return null;
        JsonElement value = parent.get(name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static List<String> withExtensions(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png") || lower.endsWith(".tga") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return List.of(path);
        return List.of(path + ".png", path + ".tga", path + ".jpg", path + ".jpeg");
    }

}
