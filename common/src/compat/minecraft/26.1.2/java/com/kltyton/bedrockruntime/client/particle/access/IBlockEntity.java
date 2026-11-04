package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;

public interface IBlockEntity extends IPSBlockEntity {
    VariableTable kltyton_bedrock_runtime$getVariableTable();

    @Override
    default Vec3 getPos() {
        var pos = ((BlockEntity) this).getBlockPos();
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Override
    default @Nullable Level getLevel() {
        return ((BlockEntity) this).getLevel();
    }

    @Override
    default boolean isDiscarded() {
        return ((BlockEntity) this).isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return kltyton_bedrock_runtime$getVariableTable();
    }

    static IBlockEntity of(BlockEntity blockEntity) {
        return (IBlockEntity) blockEntity;
    }
}
