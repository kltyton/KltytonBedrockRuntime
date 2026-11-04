package com.kltyton.bedrockruntime.client.particle.runtime.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.event.ParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/// Emitter attached to a block position; removes itself when the block state changes away from the
/// one it was attached to.
public class WithBlockParticleEmitter extends IgnoreRangeParticleEmitter {
    public static final ResourceLocation TYPE = BedrockParticles.asResource("with_block");

    protected @Nullable WithBlockParticleEmitter.BlockData blockData;
    private boolean blockBound = true;

    public WithBlockParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock, boolean ignoreRange) {
        super(TYPE, level, pos, particleId, expression, ignoreRange);
        initBlock(level, pos, ignoreSameBlock);
    }

    public WithBlockParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        this.blockBound = (!tag.contains("blockBound") || tag.getBoolean("blockBound"));
        if (blockBound) initBlock(level, pos, tag.getBoolean("ignoreSameBlock"));
    }

    public WithBlockParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
        this.blockBound = effect.type() == ParticleEffect.Type.EMITTER_BOUND;
        if (blockBound) {
            if (parent instanceof WithBlockParticleEmitter blockEmitter && blockEmitter.blockData != null) {
                this.blockData = blockEmitter.blockData;
            } else {
                initBlock(parent.level, parent.getPosition(), false);
            }
        }
    }

    private void initBlock(Level level, Vec3 pos, boolean ignoreSameBlock) {
        BlockPos blockPos = BlockPos.containing(pos);
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            remove();
        } else {
            this.blockData = new BlockData(blockPos, state, ignoreSameBlock);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (blockData != null) {
            BlockState state = level.getBlockState(blockData.pos);
            if (state != blockData.state && (!blockData.ignoreSameBlock || state.getBlock() != blockData.state.getBlock())) {
                remove();
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return super.isRemoved() || blockBound && blockData == null;
    }

    @Override
    public void serialize(CompoundTag tag) {
        super.serialize(tag);
        tag.putBoolean("blockBound", blockBound);
        if (blockData != null) {
            tag.putBoolean("ignoreSameBlock", blockData.ignoreSameBlock);
        }
    }

    public record BlockData(BlockPos pos, BlockState state, boolean ignoreSameBlock) {}

    public @Nullable BlockData getBlockData() {
        return blockData;
    }
}
