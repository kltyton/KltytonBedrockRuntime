package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.client.particle.access.ITextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements ITextureAtlasSprite {
    @Unique
    private int kltyton_bedrock_runtime$originX;
    @Unique
    private int kltyton_bedrock_runtime$originY;

    @Override
    public int kltyton_bedrock_runtime$getOriginX() {
        return kltyton_bedrock_runtime$originX;
    }

    @Override
    public int kltyton_bedrock_runtime$getOriginY() {
        return kltyton_bedrock_runtime$originY;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$storeOrigin(ResourceLocation atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, CallbackInfo ci) {
        this.kltyton_bedrock_runtime$originX = atlasWidth;
        this.kltyton_bedrock_runtime$originY = atlasHeight;
    }
}
