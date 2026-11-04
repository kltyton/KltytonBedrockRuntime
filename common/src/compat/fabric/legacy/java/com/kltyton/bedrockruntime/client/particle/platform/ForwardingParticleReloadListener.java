package com.kltyton.bedrockruntime.client.particle.platform;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Preserves the native asynchronous prepare/barrier/apply contract on Fabric 1.20/1.21. */
record ForwardingParticleReloadListener(ResourceLocation id, PreparableReloadListener delegate)
        implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resources,
                                           ProfilerFiller preparationProfiler, ProfilerFiller applyProfiler,
                                           Executor preparationExecutor, Executor applyExecutor) {
        return delegate.reload(barrier, resources, preparationProfiler, applyProfiler,
                preparationExecutor, applyExecutor);
    }
}
