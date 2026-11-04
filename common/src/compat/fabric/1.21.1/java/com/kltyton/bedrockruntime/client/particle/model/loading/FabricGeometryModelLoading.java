package com.kltyton.bedrockruntime.client.particle.model.loading;

import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.geometry.GeometryParticleSpriteSource;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleLoader;
import com.kltyton.bedrockruntime.mixin.client.particle.SpriteSourcesAccessor;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricGeometryModelLoading {
    private static final Set<ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();

    private FabricGeometryModelLoading() {
    }

    public static void initialize() {
        registerSpriteSource();
        PreparableModelLoadingPlugin.<Map<String, String>>register(
                (resources, executor) -> CompletableFuture.supplyAsync(
                        () -> ModelParticleLoader.geometryModelIds(resources), executor),
                (models, context) -> {
                    NativeGeometryParticleBakery.clear();
                    REGISTERED.clear();
                    for (String modelId : new LinkedHashSet<>(models.values())) {
                        ResourceLocation location = ResourceLocation.tryParse(modelId);
                        if (location == null) {
                            continue;
                        }
                        REGISTERED.add(location);
                        context.addModels(location);
                    }
                    context.modifyModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, baked) -> {
                        ResourceLocation id = baked.resourceId();
                        if (id != null && REGISTERED.contains(id)) {
                            NativeGeometryParticleBakery.publishFromModel(id.toString(), model);
                        }
                        return model;
                    });
                });
    }

    private static void registerSpriteSource() {
        SpriteSourceType sourceType = new SpriteSourceType(GeometryParticleSpriteSource.codec());
        if (SpriteSourcesAccessor.kltyton_bedrock_runtime$getTypes()
                .putIfAbsent(BedrockParticles.asResource("geometry_particles"), sourceType) != null) {
            throw new IllegalStateException("Duplicate geometry particle sprite source registration");
        }
        GeometryParticleSpriteSource.bindType(sourceType);
    }
}
