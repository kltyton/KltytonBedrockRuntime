package com.kltyton.bedrockruntime.client.selection.model;

import com.kltyton.bedrockruntime.client.render.capture.EntityCapturePose;

/**
 * CN: 在 render state 中保留源实体 id 和提取时的根姿势。
 * EN: Retains source entity identity and the root pose sampled during render-state extraction.
 */
public interface VisualPartEntityRenderState {
    int kltyton_bedrock_runtime$getEntityId();

    void kltyton_bedrock_runtime$setEntityId(int entityId);

    EntityCapturePose kltyton_bedrock_runtime$getCapturePose();

    void kltyton_bedrock_runtime$setCapturePose(EntityCapturePose pose);
}
