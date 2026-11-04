package com.kltyton.bedrockruntime.client.particle.model.resource;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticlePrepared;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleTextures;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Resource reload listener that prepares JSON/textures off-thread and publishes both on the game thread. */
public final class ModelParticleReloadListener implements PreparableReloadListener {
    public static final ModelParticleReloadListener INSTANCE = new ModelParticleReloadListener();

    private ModelParticleReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor,
                                          PreparationBarrier preparationBarrier, Executor gameExecutor) {
        ResourceManager resources = sharedState.resourceManager();
        TextureAtlas atlas = ModelParticleTextures.createAtlas();
        int maxMipLevel = ModelParticleTextures.maxMipLevel();
        CompletableFuture<ReloadData> data = CompletableFuture
                .supplyAsync(() -> ModelParticleLoader.prepare(resources), backgroundExecutor)
                .thenCompose(models -> ModelParticleTextures
                        .prepare(resources, models, atlas, maxMipLevel, backgroundExecutor)
                        .thenApply(textures -> new ReloadData(models, textures)));
        return data.thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(reload -> {
                    ModelParticleTextures.apply(reload.textures());
                    ModelParticleLoader.apply(reload.models());
                }, gameExecutor);
    }

    private record ReloadData(ModelParticlePrepared models, ModelParticleTextures.Prepared textures) {
    }
}
