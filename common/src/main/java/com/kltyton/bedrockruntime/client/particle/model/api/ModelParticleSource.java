package com.kltyton.bedrockruntime.client.particle.model.api;

/**
 * Lazy source for model geometry. Version adapters use it to defer GeckoLib/vanilla lookups until
 * the client resource manager and model bakery are ready.
 */
@FunctionalInterface
public interface ModelParticleSource {
    ModelParticleModel resolve();

    static ModelParticleSource fixed(ModelParticleModel model) {
        return () -> model;
    }
}
