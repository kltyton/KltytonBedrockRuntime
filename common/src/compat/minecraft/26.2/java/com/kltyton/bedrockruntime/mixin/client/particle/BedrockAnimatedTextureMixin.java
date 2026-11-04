package com.kltyton.bedrockruntime.mixin.client.particle;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.texture.SpriteContents;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockAnimationState;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the animation state created for a Bedrock terrain sprite. */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimatedTexture")
public abstract class BedrockAnimatedTextureMixin {
    @Shadow @Final SpriteContents this$0;

    @Inject(method = "createAnimationState", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$markTerrainAnimation(GpuBufferSlice slice, int spriteUboSize,
            CallbackInfoReturnable<SpriteContents.AnimationState> callback) {
        var id = this$0.name();
        if (id.getNamespace().equals(BedrockPackResources.NAMESPACE)
                && id.getPath().startsWith("bedrock_terrain/animated/")) {
            ((BedrockAnimationState) (Object) callback.getReturnValue()).kltyton_bedrock_runtime$markBedrockTerrain();
        }
    }
}
