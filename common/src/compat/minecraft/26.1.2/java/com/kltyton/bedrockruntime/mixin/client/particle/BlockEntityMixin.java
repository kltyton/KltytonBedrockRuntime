package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.world.level.block.entity.BlockEntity;
import com.kltyton.bedrockruntime.client.particle.api.AddDefaultVariableEvent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.access.IBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements IBlockEntity {
    @Unique
    private VariableTable kltyton_bedrock_runtime$variableTable;

    @Override
    public VariableTable kltyton_bedrock_runtime$getVariableTable() {
        if (kltyton_bedrock_runtime$variableTable == null) {
            this.kltyton_bedrock_runtime$variableTable = new VariableTable(null);
            com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents.post(new AddDefaultVariableEvent.BlockEntity(kltyton_bedrock_runtime$variableTable, (BlockEntity) (Object) this));
        }
        return kltyton_bedrock_runtime$variableTable;
    }
}
