package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ConcurrentHashMap;

/** Native culled entity pipelines with material-specific texture sampling. */
public final class ModelParticleRenderTypes {
    private static final ConcurrentHashMap<Key, RenderType> TYPES = new ConcurrentHashMap<>();

    private ModelParticleRenderTypes() {
    }

    public static RenderType forMaterial(ModelMaterial material, Identifier atlas) {
        return TYPES.computeIfAbsent(new Key(material, atlas), ModelParticleRenderTypes::create);
    }

    private static RenderType create(Key key) {
        var pipeline = switch (key.material()) {
            case SOLID -> RenderPipelines.ENTITY_SOLID;
            case CUTOUT, CUTOUT_MIPPED -> RenderPipelines.ENTITY_CUTOUT_CULL;
            case TRANSLUCENT -> RenderPipelines.ENTITY_TRANSLUCENT_CULL;
        };
        boolean mipped = key.material() == ModelMaterial.CUTOUT_MIPPED;
        var setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", key.atlas(),
                        () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, mipped))
                .useLightmap()
                .useOverlay();
        if (key.material() == ModelMaterial.TRANSLUCENT) {
            setup.sortOnUpload();
        }
        return RenderType.create("kltyton_bedrock_runtime_model_" + key.material().name().toLowerCase(java.util.Locale.ROOT),
                setup.createRenderSetup());
    }

    private record Key(ModelMaterial material, Identifier atlas) {
    }
}

