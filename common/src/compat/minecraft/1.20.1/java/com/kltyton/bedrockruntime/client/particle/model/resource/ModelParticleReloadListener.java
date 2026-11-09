package com.kltyton.bedrockruntime.client.particle.model.resource;

import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticlePrepared;
import com.kltyton.bedrockruntime.client.particle.model.runtime.ModelParticleTextures;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** 1.20.1 resource reload listener; JSON/texture reads run off-thread, upload/publish on the game thread. */
public final class ModelParticleReloadListener implements PreparableReloadListener {
    public static final ModelParticleReloadListener INSTANCE = new ModelParticleReloadListener();

    private ModelParticleReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resources,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        TextureAtlas atlas = ModelParticleTextures.createAtlas();
        int maxMipLevel = ModelParticleTextures.maxMipLevel();
        CompletableFuture<ReloadData> data = CompletableFuture
                .supplyAsync(() -> ModelParticleLoader.prepare(resources), backgroundExecutor)
                .thenCompose(models -> ModelParticleTextures
                        .prepare(resources, models, atlas, maxMipLevel, backgroundExecutor)
                        .thenApply(textures -> new ReloadData(models, textures)));
        return data.thenCompose(barrier::wait)
                .thenAcceptAsync(reload -> {
                    ModelParticleTextures.apply(reload.textures());
                    ModelParticleLoader.apply(reload.models());
                    BonePartSelectionClient.invalidateRenderCaptures();
                }, gameExecutor);
    }

    private record ReloadData(ModelParticlePrepared models, ModelParticleTextures.Prepared textures) {
    }
}
