package com.kltyton.bedrockruntime.client.particle.model.runtime;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelMaterial;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;

/** Uses native entity materials; only the mip-sampling variant needs its own texture state. */
public final class ModelParticleRenderTypes extends RenderType {
    private static final ConcurrentHashMap<ResourceLocation, RenderType> MIPPED = new ConcurrentHashMap<>();

    private ModelParticleRenderTypes(ResourceLocation atlas) {
        this(RenderType.entityCutout(atlas), new TextureStateShard(atlas, false, true));
    }

    private ModelParticleRenderTypes(RenderType base, TextureStateShard texture) {
        super("kltyton_bedrock_runtime_model_cutout_mipped", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, false,
                () -> {
                    base.setupRenderState();
                    texture.setupRenderState();
                }, base::clearRenderState);
    }

    public static RenderType forMaterial(ModelMaterial material, ResourceLocation atlas) {
        return switch (material) {
            case SOLID -> RenderType.entitySolid(atlas);
            case CUTOUT -> RenderType.entityCutout(atlas);
            case CUTOUT_MIPPED -> MIPPED.computeIfAbsent(atlas, ModelParticleRenderTypes::new);
            case TRANSLUCENT -> RenderType.entityTranslucentCull(atlas);
        };
    }
}
