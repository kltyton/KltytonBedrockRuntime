package com.kltyton.bedrockruntime.client.particle.model.loading;

import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.geometry.GeometryParticleSpriteSource;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleLoader;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class FabricGeometryModelLoading {
    private FabricGeometryModelLoading() {
    }

    public static void initialize() {
        SpriteSourceRegistry.register(BedrockParticles.asResource("geometry_particles"),
                GeometryParticleSpriteSource.CODEC);
        PreparableModelLoadingPlugin.<Map<String, String>>register(
                (state, executor) -> CompletableFuture.supplyAsync(
                        () -> ModelParticleLoader.geometryModelIds(state.resourceManager()), executor),
                (models, context) -> {
                    NativeGeometryParticleBakery.clear();
                    for (String modelId : new LinkedHashSet<>(models.values())) {
                        Identifier id = Identifier.tryParse(modelId);
                        if (id == null) {
                            continue;
                        }
                        ExtraModelKey<QuadCollection> key = ExtraModelKey.create(id::toString);
                        context.addModel(key, new SimpleUnbakedExtraModel<>(id, (resolved, baker) -> {
                            QuadCollection quads = resolved.bakeTopGeometry(resolved.getTopTextureSlots(),
                                    baker, BlockModelRotation.IDENTITY);
                            NativeGeometryParticleBakery.publishQuads(id.toString(), quads.getAll());
                            return quads;
                        }));
                    }
                });
    }
}
