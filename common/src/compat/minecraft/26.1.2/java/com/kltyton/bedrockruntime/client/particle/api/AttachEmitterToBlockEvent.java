package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.datafixers.util.Function3;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.WithBlockParticleEmitter;

import java.util.Map;

/// Fired on the mod event bus so mods can attach BedrockParticles emitters to block animate ticks.
public class AttachEmitterToBlockEvent {
    private final Map<BlockState, EmitterAttachHandler.AttachData> stateMap;
    private final Map<Block, EmitterAttachHandler.AttachData> blockMap;

    @ApiStatus.Internal
    public AttachEmitterToBlockEvent(Map<BlockState, EmitterAttachHandler.AttachData> stateMap, Map<Block, EmitterAttachHandler.AttachData> blockMap) {
        this.stateMap = stateMap;
        this.blockMap = blockMap;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData.Wrapped(factory, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData.Wrapped(factory, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, Identifier particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, Identifier particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, Identifier particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, Identifier particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }
}
