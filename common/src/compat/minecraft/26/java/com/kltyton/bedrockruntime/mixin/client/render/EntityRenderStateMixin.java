package com.kltyton.bedrockruntime.mixin.client.render;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartEntityRenderState;
import com.kltyton.bedrockruntime.client.render.capture.EntityCapturePose;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * CN: 保存源实体 id 和根姿势，使延迟捕获使用同一次渲染状态提取的坐标。
 * EN: Stores identity and root pose so deferred capture uses the same render-state extraction coordinates.
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements VisualPartEntityRenderState {
    @Unique
    private int kltyton_bedrock_runtime$entityId = -1;

    @Unique
    private EntityCapturePose kltyton_bedrock_runtime$capturePose;

    @Override
    public EntityCapturePose kltyton_bedrock_runtime$getCapturePose() {
        return kltyton_bedrock_runtime$capturePose;
    }

    @Override
    public void kltyton_bedrock_runtime$setCapturePose(EntityCapturePose pose) {
        kltyton_bedrock_runtime$capturePose = pose;
    }

    @Override
    public int kltyton_bedrock_runtime$getEntityId() {
        return kltyton_bedrock_runtime$entityId;
    }

    @Override
    public void kltyton_bedrock_runtime$setEntityId(int entityId) {
        kltyton_bedrock_runtime$entityId = entityId;
    }
}
