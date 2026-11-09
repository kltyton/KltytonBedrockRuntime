package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;

/** Applies Bedrock alpha blending at the shared upload boundary, after renderer-specific RGB interpolation. */
@Mixin(SpriteContents.class)
public abstract class BedrockTerrainInterpolationMixin {
    @Shadow @Final private ResourceLocation name;
    @Shadow @Final private int width;
    @Shadow @Final private int height;
    @Shadow private NativeImage[] byMipLevel;

    @Unique private Map<NativeImage[], SpriteContents.Ticker> kltyton_bedrock_runtime$interpolators;

    @Inject(method = "createTicker", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$trackInterpolation(CallbackInfoReturnable<SpriteTicker> cir) {
        if (!name.getNamespace().equals(BedrockPackResources.NAMESPACE)
                || !name.getPath().startsWith("bedrock_terrain/animated/")) return;
        if (cir.getReturnValue() instanceof SpriteContents.Ticker ticker && ticker.interpolationData != null) {
            if (kltyton_bedrock_runtime$interpolators == null)
                kltyton_bedrock_runtime$interpolators = new IdentityHashMap<>();
            kltyton_bedrock_runtime$interpolators.put(ticker.interpolationData.activeFrame, ticker);
        }
    }

    @Inject(method = "upload", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$blendTerrain(int atlasX, int atlasY, int frameX, int frameY,
            NativeImage[] images, CallbackInfo ci) {
        if (kltyton_bedrock_runtime$interpolators == null) return;
        SpriteContents.Ticker ticker = kltyton_bedrock_runtime$interpolators.get(images);
        if (ticker == null) return;
        SpriteContents.AnimatedTexture animation = ticker.animationInfo;
        SpriteContents.FrameInfo current = animation.frames.get(ticker.frame);
        int nextIndex = animation.frames.get((ticker.frame + 1) % animation.frames.size()).index;
        double currentWeight = 1.0 - (double) ticker.subFrame / current.time;
        for (int level = 0; level < images.length; level++) {
            int frameWidth = width >> level;
            int frameHeight = height >> level;
            NativeImage source = byMipLevel[level];
            NativeImage destination = images[level];
            int currentX = (current.index % animation.frameRowSize * width) >> level;
            int currentY = (current.index / animation.frameRowSize * height) >> level;
            int nextX = (nextIndex % animation.frameRowSize * width) >> level;
            int nextY = (nextIndex / animation.frameRowSize * height) >> level;
            for (int y = 0; y < frameHeight; y++) {
                for (int x = 0; x < frameWidth; x++) {
                    int first = source.getPixelRGBA(currentX + x, currentY + y);
                    int second = source.getPixelRGBA(nextX + x, nextY + y);
                    int firstAlpha = first >>> 24;
                    int secondAlpha = second >>> 24;
                    if (firstAlpha == 255 && secondAlpha == 255) continue;
                    int pixel;
                    if (firstAlpha < 3) pixel = second;
                    else if (secondAlpha < 3) pixel = first;
                    else {
                        int alpha = (int) (currentWeight * firstAlpha + (1.0 - currentWeight) * secondAlpha);
                        pixel = destination.getPixelRGBA(x, y) & 0x00FFFFFF | alpha << 24;
                    }
                    destination.setPixelRGBA(x, y, pixel);
                }
            }
        }
    }
}
