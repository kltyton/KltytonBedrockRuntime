package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(targets = "net.minecraft.client.particle.ParticleEngine$MutableSpriteSet")
public interface MutableSpriteSetPreviewAccessor {
    @Accessor("sprites")
    @Nullable
    List<TextureAtlasSprite> kltyton_bedrock_runtime$getSprites();

    @Invoker("rebind")
    void kltyton_bedrock_runtime$rebind(List<TextureAtlasSprite> sprites);
}
