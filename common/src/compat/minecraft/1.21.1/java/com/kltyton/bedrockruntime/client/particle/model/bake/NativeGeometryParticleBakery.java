package com.kltyton.bedrockruntime.client.particle.model.bake;

import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModel;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModelType;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticlePart;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelUvSpace;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleTextures;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import com.kltyton.bedrockruntime.geometry.mesh.TriangleMeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class NativeGeometryParticleBakery {
    private static final int STRIDE = 8;
    private static final Map<String, List<BakedQuad>> BAKED = new java.util.concurrent.ConcurrentHashMap<>();

    private NativeGeometryParticleBakery() {
    }

    public static void clear() {
        BAKED.clear();
    }

    public static void publish(String modelId, List<BakedQuad> quads) {
        if (modelId == null || modelId.isBlank() || quads == null) {
            return;
        }
        BAKED.put(modelId, List.copyOf(quads));
    }

    public static void publishFromModel(String modelId, BakedModel model) {
        if (model == null) {
            BAKED.remove(modelId);
            return;
        }
        RandomSource random = RandomSource.create(0L);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(null, direction, random));
        }
        quads.addAll(model.getQuads(null, null, random));
        publish(modelId, quads);
    }

    public static ModelParticleModel bake(String id, String modelId, ModelParticleModelType type) {
        List<BakedQuad> quads = BAKED.get(modelId);
        if (quads == null) {
            throw new IllegalArgumentException(id + ": native geometry model was not baked: " + modelId);
        }
        return fromQuads(id, quads, type);
    }

    public static ModelParticleModel bakeJson(String id, JsonObject json, ModelParticleModelType type) {
        if (json == null) {
            throw new IllegalArgumentException(id + ": missing geometry json");
        }
        try {
            ResourceLocation rootId = ResourceLocation.tryParse(type.modelId());
            if (rootId == null) {
                rootId = ResourceLocation.fromNamespaceAndPath("minecraft", "dynamic/" + id.replace(':', '/'));
            }
            BlockModel model = BlockModel.fromStream(new StringReader(json.toString()));
            Map<ResourceLocation, UnbakedModel> cache = new HashMap<>();
            cache.put(rootId, model);
            model.resolveParents(location -> loadParentModel(cache, location, id));
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
            Function<Material, TextureAtlasSprite> sprites = material -> {
                TextureAtlasSprite sprite = ModelParticleTextures.sprite(material.texture().toString());
                return sprite == null ? missing : sprite;
            };
            BakedModel baked = model.bake(new JsonModelBaker(cache, sprites), sprites, BlockModelRotation.X0_Y0);
            if (baked == null) {
                throw new IllegalArgumentException(id + ": native geometry bake returned no model");
            }
            return fromModel(id, baked, type);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(id + ": failed to bake native geometry json", exception);
        }
    }

    public static ModelParticleModel fromModel(String id, BakedModel model, ModelParticleModelType type) {
        RandomSource random = RandomSource.create(0L);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(null, direction, random));
        }
        quads.addAll(model.getQuads(null, null, random));
        return fromQuads(id, quads, type);
    }

    private static ModelParticleModel fromQuads(String id, List<BakedQuad> quads, ModelParticleModelType type) {
        if (quads.isEmpty()) {
            throw new IllegalArgumentException(id + ": native geometry model produced no quads");
        }
        Map<String, TriangleMeshBuilder> builders = new LinkedHashMap<>();
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            if (vertices == null || vertices.length < 4 * STRIDE) {
                continue;
            }
            TextureAtlasSprite sprite = quad.getSprite();
            String texture = partTexture(sprite);
            TriangleMeshBuilder builder = builders.computeIfAbsent(texture, ignored -> new TriangleMeshBuilder());
            int[] positions = new int[4];
            int[] normals = new int[4];
            int[] uvs = new int[4];
            float normalX = 0.0F;
            float normalY = 1.0F;
            float normalZ = 0.0F;
            Direction direction = quad.getDirection();
            if (direction != null) {
                var normal = direction.getNormal();
                normalX = normal.getX();
                normalY = normal.getY();
                normalZ = normal.getZ();
            }
            int packedNormal = vertices[7];
            if (packedNormal != 0) {
                float customX = (byte) (packedNormal & 0xFF) / 127.0F;
                float customY = (byte) ((packedNormal >> 8) & 0xFF) / 127.0F;
                float customZ = (byte) ((packedNormal >> 16) & 0xFF) / 127.0F;
                if (customX * customX + customY * customY + customZ * customZ > 1.0E-6F) {
                    normalX = customX;
                    normalY = customY;
                    normalZ = customZ;
                }
            }
            for (int corner = 0; corner < 4; corner++) {
                int base = corner * STRIDE;
                float x = Float.intBitsToFloat(vertices[base]);
                float y = Float.intBitsToFloat(vertices[base + 1]);
                float z = Float.intBitsToFloat(vertices[base + 2]);
                float u = Float.intBitsToFloat(vertices[base + 4]);
                float v = Float.intBitsToFloat(vertices[base + 5]);
                positions[corner] = builder.addPosition(x, y, z,
                        abgrToArgb(vertices[base + 3]), vertices[base + 6]);
                normals[corner] = builder.addNormal(normalX, normalY, normalZ);
                uvs[corner] = builder.addUv(normalizedU(u, sprite), normalizedV(v, sprite));
            }
            builder.addQuad(positions[0], positions[1], positions[2], positions[3],
                    normals[0], normals[1], normals[2], normals[3],
                    uvs[0], uvs[1], uvs[2], uvs[3]);
        }
        List<ModelParticlePart> parts = new ArrayList<>();
        for (Map.Entry<String, TriangleMeshBuilder> entry : builders.entrySet()) {
            TriangleMesh mesh = entry.getValue().build();
            if (mesh.triangleCount() > 0) {
                parts.add(new ModelParticlePart(entry.getKey(), type.material(), ModelUvSpace.NORMALIZED,
                        mesh, new Matrix4f(), type.doubleSided()));
            }
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException(id + ": native geometry model produced no drawable quads");
        }
        return new ModelParticleModel(id, type.texture(), parts, type.material(), type.doubleSided());
    }

    private static String partTexture(TextureAtlasSprite sprite) {
        if (sprite != null) {
            ResourceLocation name = sprite.contents().name();
            if (name != null) {
                return name.toString();
            }
        }
        return MissingTextureAtlasSprite.getLocation().toString();
    }

    private static int abgrToArgb(int packed) {
        int alpha = (packed >>> 24) & 0xFF;
        int blue = (packed >>> 16) & 0xFF;
        int green = (packed >>> 8) & 0xFF;
        int red = packed & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float normalizedU(float value, TextureAtlasSprite sprite) {
        if (sprite == null) {
            return value;
        }
        float span = sprite.getU1() - sprite.getU0();
        return Math.abs(span) <= 1.0E-6F ? value : (value - sprite.getU0()) / span;
    }

    private static float normalizedV(float value, TextureAtlasSprite sprite) {
        if (sprite == null) {
            return value;
        }
        float span = sprite.getV1() - sprite.getV0();
        return Math.abs(span) <= 1.0E-6F ? value : (value - sprite.getV0()) / span;
    }

    private static UnbakedModel loadParentModel(Map<ResourceLocation, UnbakedModel> cache,
                                                ResourceLocation location, String owner) {
        UnbakedModel existing = cache.get(location);
        if (existing != null) {
            return existing;
        }
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(location.getNamespace(),
                "models/" + location.getPath() + ".json");
        Resource resource = resources.getResource(file).orElseThrow(
                () -> new IllegalArgumentException(owner + ": missing parent model " + location));
        try (Reader reader = resource.openAsReader()) {
            BlockModel parent = BlockModel.fromStream(reader);
            cache.put(location, parent);
            return parent;
        } catch (Exception exception) {
            throw new IllegalArgumentException(owner + ": cannot read parent model " + location, exception);
        }
    }

    private static final class JsonModelBaker implements ModelBaker {
        private final Map<ResourceLocation, UnbakedModel> models;
        private final Function<Material, TextureAtlasSprite> sprites;

        private JsonModelBaker(Map<ResourceLocation, UnbakedModel> models,
                               Function<Material, TextureAtlasSprite> sprites) {
            this.models = models;
            this.sprites = sprites;
        }

        public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
            return sprites;
        }

        @Override
        public UnbakedModel getModel(ResourceLocation location) {
            UnbakedModel model = models.get(location);
            if (model == null) {
                throw new IllegalArgumentException("Unresolved geometry model " + location);
            }
            return model;
        }

        @Override
        public BakedModel bake(ResourceLocation location, ModelState state) {
            return bake(location, state, sprites);
        }

        public BakedModel bake(ResourceLocation location, ModelState state,
                               Function<Material, TextureAtlasSprite> textureGetter) {
            return getModel(location).bake(this, textureGetter, state);
        }

        public BakedModel bakeUncached(UnbakedModel model, ModelState state,
                                       Function<Material, TextureAtlasSprite> textureGetter) {
            return model.bake(this, textureGetter, state);
        }

        public UnbakedModel getTopLevelModel(net.minecraft.client.resources.model.ModelResourceLocation location) {
            return getModel(location.id());
        }
    }
}
