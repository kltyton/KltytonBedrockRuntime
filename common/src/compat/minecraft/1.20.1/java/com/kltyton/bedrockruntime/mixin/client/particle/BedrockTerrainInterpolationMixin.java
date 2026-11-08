package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.texture.SpriteContents;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blends transparent Bedrock frames within the native sprite upload path. */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$InterpolationData")
public abstract class BedrockTerrainInterpolationMixin {
    @Unique private boolean kltyton_bedrock_runtime$bedrockTerrain;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/texture/SpriteContents;)V", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$identifyTerrain(SpriteContents contents, CallbackInfo ci) {
        var id = contents.name();
        kltyton_bedrock_runtime$bedrockTerrain = id.getNamespace().equals(BedrockPackResources.NAMESPACE)
                && id.getPath().startsWith("bedrock_terrain/animated/");
    }

    @ModifyArg(method = "uploadInterpolatedFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"), index = 2)
    private int kltyton_bedrock_runtime$blendTerrain(int pixel, @Local(index = 16) int current,
            @Local(index = 17) int next, @Local(index = 7) double currentWeight) {
        if (!kltyton_bedrock_runtime$bedrockTerrain) return pixel;
        int currentAlpha = current >>> 24;
        int nextAlpha = next >>> 24;
        if (currentAlpha < 3) return next;
        if (nextAlpha < 3) return current;
        int alpha = (int) (currentWeight * currentAlpha + (1.0 - currentWeight) * nextAlpha);
        return pixel & 0x00FFFFFF | alpha << 24;
    }
}
