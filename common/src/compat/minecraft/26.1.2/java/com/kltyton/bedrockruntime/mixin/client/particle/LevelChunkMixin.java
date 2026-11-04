package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockHeightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Invalidates particle height columns after actual client chunk changes. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$invalidateHeight(BlockPos position, BlockState state, int flags,
            CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel().isClientSide() && cir.getReturnValue() != null) {
            BedrockHeightmap.blockChanged(chunk, position);
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$replaceHeights(CallbackInfo ci) {
        BedrockHeightmap.chunkReplaced((LevelChunk) (Object) this);
    }
}
