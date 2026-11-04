package com.kltyton.bedrockruntime.client.particle.molang.query;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Samples model-data-aware quads from the block's actual NeoForge render layers. */
final class SurfaceParticleModelSampler {
    private SurfaceParticleModelSampler() {}

    static TextureAtlasSprite sample(BlockState state, ClientLevel level, BlockPos pos, Direction face) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        ModelData data = model.getModelData(level, pos, state, level.getModelDataManager().getAt(pos));
        long seed = state.getSeed(pos);
        RandomSource random = RandomSource.create(seed);
        for (RenderType layer : model.getRenderTypes(state, random, data)) {
            random.setSeed(seed);
            for (BakedQuad quad : model.getQuads(state, face, random, data, layer)) {
                if (quad.getDirection() == face) return quad.getSprite();
            }
            random.setSeed(seed);
            for (BakedQuad quad : model.getQuads(state, null, random, data, layer)) {
                if (quad.getDirection() == face) return quad.getSprite();
            }
        }
        return model.getParticleIcon(data);
    }
}
