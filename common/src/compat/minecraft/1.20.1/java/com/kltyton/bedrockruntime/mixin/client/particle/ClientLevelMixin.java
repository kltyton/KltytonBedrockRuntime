package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Routes vanilla per-block ambient particles to registered Bedrock emitters. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @WrapWithCondition(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean kltyton_bedrock_runtime$takeOver(Block block, BlockState state, Level level, BlockPos pos,
                                                     RandomSource random) {
        return EmitterAttachHandler.attachTo(block, state, level, pos);
    }
}
