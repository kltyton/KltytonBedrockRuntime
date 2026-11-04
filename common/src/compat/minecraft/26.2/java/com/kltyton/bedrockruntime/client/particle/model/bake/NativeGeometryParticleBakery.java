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
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeGeometryParticleBakery {
    private static final Map<String, List<BakedQuad>> BAKED = new java.util.concurrent.ConcurrentHashMap<>();

    private NativeGeometryParticleBakery() {
    }

    public static void clear() {
        BAKED.clear();
    }

    public static void publishQuads(String modelId, List<BakedQuad> quads) {
        if (modelId == null || modelId.isBlank() || quads == null) {
            return;
        }
        BAKED.put(modelId, List.copyOf(quads));
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
            Identifier rootId = normalizedModelId(type.modelId());
            if (rootId == null) {
                rootId = Identifier.fromNamespaceAndPath("minecraft", "dynamic/" + id.replace(':', '/'));
            }
            CuboidModel model = CuboidModel.fromStream(new StringReader(json.toString()));
            ResolvedModel resolved = resolveModel(rootId, model, new HashMap<>(), new LinkedHashSet<>(), id);
            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            TextureAtlasSprite missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
            if (missing == null) {
                missing = atlas.getSprite(Identifier.withDefaultNamespace("missingno"));
            }
            if (missing == null) {
                throw new IllegalArgumentException(id + ": block atlas has no missing sprite");
            }
            ModelBaker baker = new JsonModelBaker(resolved, atlas, missing);
            QuadCollection quads = resolved.bakeTopGeometry(resolved.getTopTextureSlots(), baker,
                    BlockModelRotation.IDENTITY);
            return fromQuads(id, quads.getAll(), type);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(id + ": failed to bake native geometry json", exception);
        }
    }

    private static ModelParticleModel fromQuads(String id, List<BakedQuad> quads, ModelParticleModelType type) {
        if (quads == null || quads.isEmpty()) {
            throw new IllegalArgumentException(id + ": native geometry model produced no quads");
        }
        Map<String, TriangleMeshBuilder> builders = new LinkedHashMap<>();
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            String texture = partTexture(sprite);
            TriangleMeshBuilder builder = builders.computeIfAbsent(texture, ignored -> new TriangleMeshBuilder());
            int[] positions = new int[4];
            int[] normals = new int[4];
            int[] uvs = new int[4];
            Direction direction = quad.direction();
            float normalX = direction == null ? 0.0F : (float) direction.getUnitVec3().x;
            float normalY = direction == null ? 1.0F : (float) direction.getUnitVec3().y;
            float normalZ = direction == null ? 0.0F : (float) direction.getUnitVec3().z;
            for (int corner = 0; corner < 4; corner++) {
                Vector3fc position = quad.position(corner);
                long packedUv = quad.packedUV(corner);
                positions[corner] = builder.addPosition(position.x(), position.y(), position.z());
                normals[corner] = builder.addNormal(normalX, normalY, normalZ);
                float u0 = sprite == null ? 0.0F : sprite.getU0();
                float u1 = sprite == null ? 1.0F : sprite.getU1();
                float v0 = sprite == null ? 0.0F : sprite.getV0();
                float v1 = sprite == null ? 1.0F : sprite.getV1();
                uvs[corner] = builder.addUv(normalized(UVPair.unpackU(packedUv), u0, u1),
                        normalized(UVPair.unpackV(packedUv), v0, v1));
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
            Identifier name = sprite.contents().name();
            if (name != null) {
                return name.toString();
            }
        }
        return MissingTextureAtlasSprite.getLocation().toString();
    }

    private static float normalized(float value, float minimum, float maximum) {
        float span = maximum - minimum;
        return Math.abs(span) <= 1.0E-6F ? value : (value - minimum) / span;
    }

    private static ResolvedModel resolveModel(Identifier id, CuboidModel model,
                                              Map<Identifier, ResolvedModel> cache,
                                              Set<Identifier> visiting, String owner) {
        ResolvedModel existing = cache.get(id);
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(id)) {
            throw new IllegalArgumentException(owner + ": parent loop while resolving " + id);
        }
        ResolvedModel parent = null;
        Identifier parentId = model.parent();
        if (parentId != null) {
            parent = cache.get(parentId);
            if (parent == null) {
                parent = resolveModel(parentId, loadParentModel(parentId, owner), cache, visiting, owner);
            }
        }
        ResolvedModel resolved = new Node(model, parent, id.toString());
        cache.put(id, resolved);
        visiting.remove(id);
        return resolved;
    }

    private static CuboidModel loadParentModel(Identifier location, String owner) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        Identifier file = Identifier.fromNamespaceAndPath(location.getNamespace(),
                "models/" + location.getPath() + ".json");
        Resource resource = resources.getResource(file).orElseThrow(
                () -> new IllegalArgumentException(owner + ": missing parent model " + location));
        try (Reader reader = resource.openAsReader()) {
            return CuboidModel.fromStream(reader);
        } catch (Exception exception) {
            throw new IllegalArgumentException(owner + ": cannot read parent model " + location, exception);
        }
    }

    private static Identifier normalizedModelId(String modelId) {
        Identifier location = Identifier.tryParse(modelId);
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        if (path.isBlank()) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(location.getNamespace(), path);
    }

    private record Node(UnbakedModel wrapped, ResolvedModel parent, String debugName) implements ResolvedModel {
    }

    private static final class JsonModelBaker implements ModelBaker {
        private final ResolvedModel root;
        private final TextureAtlas atlas;
        private final TextureAtlasSprite missing;

        private JsonModelBaker(ResolvedModel root, TextureAtlas atlas, TextureAtlasSprite missing) {
            this.root = root;
            this.atlas = atlas;
            this.missing = missing;
        }

        @Override
        public ResolvedModel getModel(Identifier location) {
            return root;
        }

        @Override
        public BlockStateModelPart missingBlockModelPart() {
            return new SimpleModelWrapper(QuadCollection.EMPTY, true, new Material.Baked(missing, false));
        }

        @Override
        public MaterialBaker materials() {
            return new MaterialBaker(missing) {
                @Override
                protected Material.Baked bake(Material material) {
                    TextureAtlasSprite sprite = ModelParticleTextures.sprite(material.sprite().toString());
                    return sprite == null ? null : new Material.Baked(sprite, material.forceTranslucent());
                }
            };
        }

        @Override
        public Interner interner() {
            return new Interner() {
                @Override
                public Vector3fc vector(Vector3fc vector) {
                    return vector;
                }

                @Override
                public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
                    return material;
                }
            };
        }

        @Override
        public <T> T compute(SharedOperationKey<T> key) {
            return key.compute(this);
        }
    }
}
