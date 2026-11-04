package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticlePrepared;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * 1.21.1 model-particle texture resolver.
 *
 * <p>Host block-atlas sprites are preferred because they already carry the user's real mip chain.
 * Only textures absent from the host atlas are stitched into a dedicated model atlas. The atlas is
 * rebuilt per reload and replaced atomically so stale sprites are never retained.</p>
 */
public final class ModelParticleTextures {
    public static final String MODEL_ATLAS_ID = "kltyton_bedrock_runtime:model_particles";
    public static final String MODEL_ATLAS_TEXTURE_ID = "kltyton_bedrock_runtime:textures/atlas/model_particles.png";

    private static final ResourceLocation ATLAS_ID =
            ResourceLocation.fromNamespaceAndPath("kltyton_bedrock_runtime", "model_particles");
    private static final ResourceLocation ATLAS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kltyton_bedrock_runtime", "textures/atlas/model_particles.png");
    private static final ResourceLocation ATLAS_DEFINITION =
            new FileToIdConverter("atlases", ".json").idToFile(ATLAS_ID);

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private ModelParticleTextures() {
    }

    public static String modelAtlasId() {
        return MODEL_ATLAS_ID;
    }

    public static ResourceLocation modelAtlasTexture() {
        return ATLAS_TEXTURE;
    }

    public static int mipLevels() {
        return snapshot.mipLevel();
    }

    public static boolean hasModelAtlas() {
        return snapshot.atlas() != null;
    }

    /** The currently published model atlas, or {@code null} when every texture came from a host atlas. */
    public static @Nullable TextureAtlas modelAtlas() {
        return snapshot.atlas();
    }

    public static @Nullable TextureAtlasSprite sprite(String texture) {
        if (texture == null || texture.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(texture);
        if (id == null) {
            return null;
        }
        Snapshot current = snapshot;
        TextureAtlas atlas = current.atlas();
        if (atlas != null && current.sprites().contains(id)) {
            TextureAtlasSprite modelSprite = atlas.getSprite(id);
            if (modelSprite != null && id.equals(modelSprite.contents().name())) {
                return modelSprite;
            }
        }
        TextureAtlasSprite host = hostSprite(id);
        if (host != null) {
            return host;
        }
        if (atlas != null) {
            return atlas.getSprite(id);
        }
        return hostSprite(ResourceLocation.withDefaultNamespace("missingno"));
    }

    public static TextureAtlas createAtlas() {
        return new TextureAtlas(ATLAS_TEXTURE);
    }

    public static int maxMipLevel() {
        return Math.max(0, Minecraft.getInstance().options.mipmapLevels().get());
    }

    public static CompletableFuture<Prepared> prepare(ResourceManager resources, ModelParticlePrepared models,
                                                      TextureAtlas atlas, int maxMipLevel, Executor executor) {
        return prepareInternal(resources, models, atlas, maxMipLevel, executor);
    }

    private static CompletableFuture<Prepared> prepareInternal(ResourceManager resources,
                                                               ModelParticlePrepared models,
                                                               TextureAtlas atlas, int maxMipLevel,
                                                               Executor executor) {
        Set<ResourceLocation> requested = new LinkedHashSet<>();
        boolean mipped = collectTextures(models, requested);
        List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation id : requested) {
            if (hostSprite(id) == null) {
                missing.add(id);
            }
        }
        if (missing.isEmpty()) {
            return CompletableFuture.completedFuture(new Prepared(null, null, Set.of(), 0));
        }
        PackResources pack = resources.listPacks().findFirst().orElse(null);
        if (pack == null) {
            throw new IllegalStateException("No resource pack is available for the model particle atlas");
        }
        int mipLevel = mipped ? maxMipLevel : 0;
        byte[] definition = atlasDefinition(missing);
        ResourceManager wrapped = wrap(resources, pack, definition);
        Set<ResourceLocation> published = Set.copyOf(missing);
        CompletableFuture<SpriteLoader.Preparations> stitched = SpriteLoader.create(atlas)
                .loadAndStitch(wrapped, ATLAS_ID, mipLevel, executor);
        return stitched
                .thenCompose(SpriteLoader.Preparations::waitForUpload)
                .thenApply(preparations -> new Prepared(atlas, preparations, published,
preparations.mipLevel()));
    }

    public static void apply(Prepared prepared) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (prepared.atlas() == null) {
            snapshot = Snapshot.EMPTY;
            textures.release(ATLAS_TEXTURE);
            return;
        }
        try {
            prepared.atlas().upload(prepared.preparations());
            prepared.atlas().updateFilter(prepared.preparations());
            textures.register(ATLAS_TEXTURE, prepared.atlas());
            snapshot = new Snapshot(prepared.atlas(), prepared.sprites(), prepared.mipLevel());
        } catch (RuntimeException exception) {
            try {
                prepared.atlas().close();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw new IllegalStateException("Failed to upload model particle atlas " + ATLAS_TEXTURE, exception);
        }
    }

    private static @Nullable TextureAtlasSprite hostSprite(ResourceLocation id) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        if (((com.kltyton.bedrockruntime.mixin.client.particle.TextureAtlasContentsAccessor) atlas)
                .kltyton_bedrock_runtime$getTexturesByName().isEmpty()) return null;
        TextureAtlasSprite sprite = atlas.getSprite(id);
        return sprite != null && id.equals(sprite.contents().name()) ? sprite : null;
    }

    private static boolean collectTextures(ModelParticlePrepared models, Set<ResourceLocation> output) {
        boolean mipped = false;
        if (models != null) {
            for (ModelParticlePrepared.PendingModel pending : models.descriptors().values()) {
                ModelParticleModelType type = pending.type();
                mipped |= type.material() == ModelMaterial.CUTOUT_MIPPED;
                addTexture(output, type.texture());
                if (type.variant() == ModelParticleModelType.Variant.GEOMETRY
                        && pending.vanillaModel() != null) {
                    collectVanillaTextures(pending.vanillaModel(), type.texture(), output);
                }
            }
        }
        for (ModelParticleModel model : ModelParticleModels.programmaticModels().values()) {
            mipped |= model.material() == ModelMaterial.CUTOUT_MIPPED;
            addTexture(output, model.texture());
            for (ModelParticlePart part : model.parts()) {
                mipped |= part.material() == ModelMaterial.CUTOUT_MIPPED;
                addTexture(output, part.texture());
            }
        }
        return mipped;
    }

    private static void collectVanillaTextures(JsonObject json, String defaultTexture, Set<ResourceLocation> output) {
        Map<String, String> textures = new LinkedHashMap<>();
        if (json.has("textures") && json.get("textures").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    textures.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        String modelTexture = defaultTexture == null || defaultTexture.isBlank()
                ? firstTexture(textures) : defaultTexture;
        addTexture(output, modelTexture);
        if (!json.has("elements") || !json.get("elements").isJsonArray()) {
            return;
        }
        for (JsonElement element : json.getAsJsonArray("elements")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("faces") || !object.get("faces").isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> face : object.getAsJsonObject("faces").entrySet()) {
                if (!face.getValue().isJsonObject()) {
                    continue;
                }
                JsonElement value = face.getValue().getAsJsonObject().get("texture");
                if (value == null || !value.isJsonPrimitive()) {
                    addTexture(output, modelTexture);
                    continue;
                }
                String texture = value.getAsString();
                if (texture.startsWith("#")) {
                    addTexture(output, textures.getOrDefault(texture.substring(1), modelTexture));
                } else {
                    addTexture(output, texture);
                }
            }
        }
    }

    private static String firstTexture(Map<String, String> textures) {
        for (String value : textures.values()) {
            if (value != null && !value.isBlank() && !value.startsWith("#")) {
                return value;
            }
        }
        return "";
    }

    private static void addTexture(Set<ResourceLocation> output, String texture) {
        if (texture == null || texture.isBlank()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(texture);
        if (id != null) {
            output.add(id);
        }
    }

    private static byte[] atlasDefinition(List<ResourceLocation> sprites) {
        JsonObject root = new JsonObject();
        JsonArray sources = new JsonArray();
        for (ResourceLocation sprite : sprites) {
            JsonObject source = new JsonObject();
            source.addProperty("type", "minecraft:single");
            source.addProperty("resource", sprite.toString());
            sources.add(source);
        }
        root.add("sources", sources);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static ResourceManager wrap(ResourceManager delegate, PackResources pack, byte[] definition) {
        return new ResourceManager() {
            @Override
            public Set<String> getNamespaces() {
                return delegate.getNamespaces();
            }

            @Override
            public Optional<Resource> getResource(ResourceLocation location) {
                if (ATLAS_DEFINITION.equals(location)) {
                    return Optional.of(new Resource(pack,
                            () -> (InputStream) new ByteArrayInputStream(definition)));
                }
                return delegate.getResource(location);
            }

            @Override
            public List<Resource> getResourceStack(ResourceLocation location) {
                if (ATLAS_DEFINITION.equals(location)) {
                    return List.of(new Resource(pack,
                            () -> (InputStream) new ByteArrayInputStream(definition)));
                }
                return delegate.getResourceStack(location);
            }

            @Override
            public Map<ResourceLocation, Resource> listResources(String directory,
                                                                 java.util.function.Predicate<ResourceLocation> filter) {
                return delegate.listResources(directory, filter);
            }

            @Override
            public Map<ResourceLocation, List<Resource>> listResourceStacks(String directory,
                                                                            java.util.function.Predicate<ResourceLocation> filter) {
                return delegate.listResourceStacks(directory, filter);
            }

            @Override
            public Stream<PackResources> listPacks() {
                return delegate.listPacks();
            }
        };
    }

    public record Prepared(@Nullable TextureAtlas atlas,
                           @Nullable SpriteLoader.Preparations preparations,
                           Set<ResourceLocation> sprites, int mipLevel) {
    }

    private record Snapshot(@Nullable TextureAtlas atlas, Set<ResourceLocation> sprites, int mipLevel) {
        private static final Snapshot EMPTY = new Snapshot(null, Set.of(), 0);
    }
}
