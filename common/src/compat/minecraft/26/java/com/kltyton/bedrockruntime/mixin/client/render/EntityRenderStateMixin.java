package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * CN: 在原版 render state 上保存源实体 id，保证视觉部位 id 稳定。
 * EN: Stores the source entity id on vanilla render states so visual part ids remain stable.
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements VisualPartEntityRenderState {
    @Unique
    private int kltyton_bedrock_runtime$entityId = -1;

    @Override
    public int kltyton_bedrock_runtime$getEntityId() {
        return kltyton_bedrock_runtime$entityId;
    }

    @Override
    public void kltyton_bedrock_runtime$setEntityId(int entityId) {
        kltyton_bedrock_runtime$entityId = entityId;
    }
}
