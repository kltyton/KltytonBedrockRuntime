package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Distinguishes an unpublished host atlas from a published atlas with a missing sprite. */
@Mixin(TextureAtlas.class)
public interface TextureAtlasContentsAccessor {
    @Accessor("texturesByName")
    Map<ResourceLocation, TextureAtlasSprite> kltyton_bedrock_runtime$getTexturesByName();
}
