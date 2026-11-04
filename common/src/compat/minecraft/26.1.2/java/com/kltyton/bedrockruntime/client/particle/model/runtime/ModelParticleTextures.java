package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
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
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * 26.2 model-particle texture resolver.
 *
 * <p>Textures already provided by the host block/item atlases keep their real mip chains. Only the
 * textures that no host atlas provides are stitched into a dedicated model atlas with a mip level
 * matching the user setting (0 is valid). The dedicated atlas is rebuilt from the real reload
 * result, uploaded on the game thread, and replaced atomically so old sprites are never retained.</p>
 */
public final class ModelParticleTextures {
    public static final String MODEL_ATLAS_ID = "kltyton_bedrock_runtime:model_particles";
    public static final String MODEL_ATLAS_TEXTURE_ID = "kltyton_bedrock_runtime:textures/atlas/model_particles.png";

    private static final Identifier ATLAS_ID = Identifier.parse(MODEL_ATLAS_ID);
    private static final Identifier ATLAS_TEXTURE = Identifier.parse(MODEL_ATLAS_TEXTURE_ID);
    private static final Identifier ATLAS_DEFINITION = new FileToIdConverter("atlases", ".json").idToFile(ATLAS_ID);

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private ModelParticleTextures() {
    }

    /** Stable model-atlas id for the main renderer and loader wiring. */
    public static String modelAtlasId() {
        return MODEL_ATLAS_ID;
    }

    /** Texture location registered with the texture manager; also exposed by sprites. */
    public static Identifier modelAtlasTexture() {
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

    /** Resolves a model texture, preferring host atlases that already carry real mip chains. */
    public static @Nullable TextureAtlasSprite sprite(String texture) {
        if (texture == null || texture.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(texture);
        if (id == null) {
            return null;
        }
        AtlasManager atlases = Minecraft.getInstance().getAtlasManager();
        Snapshot current = snapshot;
        TextureAtlas atlas = current.atlas();
        if (atlas != null && current.sprites().contains(id)) {
            TextureAtlasSprite modelSprite = atlas.getSprite(id);
            if (modelSprite != null && id.equals(modelSprite.contents().name())) {
                return modelSprite;
            }
        }
        TextureAtlasSprite host = hostSprite(atlases, AtlasIds.BLOCKS, id);
        if (host == null) {
            host = hostSprite(atlases, AtlasIds.ITEMS, id);
        }
        if (host != null) {
            return host;
        }
        if (atlas != null) {
            return atlas.getSprite(id);
        }
        return hostSprite(atlases, AtlasIds.BLOCKS, Identifier.withDefaultNamespace("missingno"));
    }

    /** Creates the atlas object on the game thread; it is not registered until apply publishes it. */
    public static TextureAtlas createAtlas() {
        return new ModelAtlas(ATLAS_TEXTURE);
    }

    /** Current user mip setting; 0 is a valid value and disables mip-chain generation. */
    public static int maxMipLevel() {
        return Math.max(0, Minecraft.getInstance().options.mipmapLevels().get());
    }

    /**
     * Background stage: collect only textures referenced by the descriptors/programmatic models and
     * stitch the ones missing from host atlases. The returned future is complete after the mip chain
     * is ready for upload.
     */
    public static CompletableFuture<Prepared> prepare(ResourceManager resources, ModelParticlePrepared models,
                                                      TextureAtlas atlas, int maxMipLevel, Executor executor) {
        return prepareInternal(resources, models, atlas, maxMipLevel, executor);
    }

    private static CompletableFuture<Prepared> prepareInternal(ResourceManager resources,
                                                               ModelParticlePrepared models,
                                                               TextureAtlas atlas, int maxMipLevel,
                                                               Executor executor) {
        Set<Identifier> requested = new LinkedHashSet<>();
        boolean mipped = collectTextures(models, requested);
        AtlasManager atlases = Minecraft.getInstance().getAtlasManager();
        boolean mipWanted = mipped && maxMipLevel > 0;
        List<Identifier> missing = new ArrayList<>();
        for (Identifier id : requested) {
            if (hostSprite(atlases, AtlasIds.BLOCKS, id) != null) {
                continue;
            }
            if (hostSprite(atlases, AtlasIds.ITEMS, id) != null && !mipWanted) {
                continue;
            }
            missing.add(id);
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
        Set<Identifier> published = Set.copyOf(missing);
        CompletableFuture<SpriteLoader.Preparations> stitched = SpriteLoader.create(atlas)
                .loadAndStitch(wrapped, ATLAS_ID, mipLevel, executor, Set.of());
        return stitched
                .thenCompose(preparations -> preparations.readyForUpload().thenApply(ignored -> preparations))
                .thenApply(preparations -> new Prepared(atlas, preparations, published,
preparations.mipLevel()));
    }

    /** Game-thread stage: register/upload the freshly prepared atlas and publish it atomically. */
    public static void apply(Prepared prepared) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (prepared.atlas() == null) {
            snapshot = Snapshot.EMPTY;
            textures.release(ATLAS_TEXTURE);
            return;
        }
        try {
            prepared.atlas().upload(prepared.preparations());
            if (prepared.atlas() instanceof ModelAtlas modelAtlas) {
                modelAtlas.setMipLevel(prepared.preparations().mipLevel());
            }
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

    private static @Nullable TextureAtlasSprite hostSprite(AtlasManager atlases, Identifier atlasId, Identifier id) {
        TextureAtlas atlas = atlases.getAtlasOrThrow(atlasId);
        TextureAtlasSprite sprite;
        try {
            sprite = atlas.getSprite(id);
        } catch (IllegalStateException notStitched) {
            // Host atlases are not yet uploaded while a reload prepares; like the old empty-atlas check,
            // report "not available" instead of failing the whole prepare stage.
            return null;
        }
        return sprite != null && id.equals(sprite.contents().name()) ? sprite : null;
    }

    private static boolean collectTextures(ModelParticlePrepared models, Set<Identifier> output) {
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

    private static void collectVanillaTextures(JsonObject json, String defaultTexture, Set<Identifier> output) {
        Map<String, String> textures = new java.util.LinkedHashMap<>();
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

    private static void addTexture(Set<Identifier> output, String texture) {
        if (texture == null || texture.isBlank()) {
            return;
        }
        Identifier id = Identifier.tryParse(texture);
        if (id != null) {
            output.add(id);
        }
    }

    private static byte[] atlasDefinition(List<Identifier> sprites) {
        JsonObject root = new JsonObject();
        JsonArray sources = new JsonArray();
        for (Identifier sprite : sprites) {
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
            public Optional<Resource> getResource(Identifier location) {
                if (ATLAS_DEFINITION.equals(location)) {
                    return Optional.of(new Resource(pack,
                            () -> (InputStream) new ByteArrayInputStream(definition)));
                }
                return delegate.getResource(location);
            }

            @Override
            public List<Resource> getResourceStack(Identifier location) {
                if (ATLAS_DEFINITION.equals(location)) {
                    return List.of(new Resource(pack,
                            () -> (InputStream) new ByteArrayInputStream(definition)));
                }
                return delegate.getResourceStack(location);
            }

            @Override
            public Map<Identifier, Resource> listResources(String directory,
                                                           java.util.function.Predicate<Identifier> filter) {
                return delegate.listResources(directory, filter);
            }

            @Override
            public Map<Identifier, List<Resource>> listResourceStacks(String directory,
                                                                      java.util.function.Predicate<Identifier> filter) {
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
                           Set<Identifier> sprites, int mipLevel) {
    }

    private record Snapshot(@Nullable TextureAtlas atlas, Set<Identifier> sprites, int mipLevel) {
        private static final Snapshot EMPTY = new Snapshot(null, Set.of(), 0);
    }

    /** Uses a mipmapped clamp sampler once the atlas actually contains a mip chain. */
    private static final class ModelAtlas extends TextureAtlas {
        private volatile int mipLevel;

        private ModelAtlas(Identifier location) {
            super(location);
        }

        private void setMipLevel(int mipLevel) {
            this.mipLevel = mipLevel;
        }

        @Override
        public GpuSampler getSampler() {
            if (mipLevel > 0) {
                return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
            }
            return super.getSampler();
        }
    }
}
