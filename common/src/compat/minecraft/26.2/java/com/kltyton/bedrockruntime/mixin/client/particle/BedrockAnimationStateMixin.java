package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.texture.SpriteContents;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleRenderPipelines;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Selects Bedrock interpolation for marked terrain animations. */
@Mixin(SpriteContents.AnimationState.class)
public abstract class BedrockAnimationStateMixin implements BedrockAnimationState {
    @Unique private boolean kltyton_bedrock_runtime$bedrockTerrain;

    @Override
    public void kltyton_bedrock_runtime$markBedrockTerrain() {
        kltyton_bedrock_runtime$bedrockTerrain = true;
    }

    @ModifyExpressionValue(method = "drawToAtlas", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/RenderPipelines;ANIMATE_SPRITE_INTERPOLATE:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline kltyton_bedrock_runtime$terrainInterpolation(RenderPipeline original) {
        RenderPipeline pipeline = ParticleRenderPipelines.bedrockTerrainAnimation();
        return kltyton_bedrock_runtime$bedrockTerrain && pipeline != null ? pipeline : original;
    }
}
