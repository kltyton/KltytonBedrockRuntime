package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.atlas.ParticleAtlasRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes the freshly uploaded atlas so pending Bedrock sprite mappings become active. */
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasUploadMixin {
    @Inject(method = "upload", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$afterUpload(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureAtlas atlas = (TextureAtlas) (Object) this;
        BedrockTerrainSpriteSource.atlasStitched(atlas);
        BedrockSpriteSource.atlasStitched(atlas);
        ParticleAtlasRevision.stitched();
    }
}
