package com.kltyton.bedrockruntime.client.selection.model;

/**
 * CN: 客户端 render-state 扩展，用于让原版模型部位 id 稳定关联实体 id。
 * EN: Client render-state extension used to keep vanilla visual part ids tied to stable entity ids.
 */
public interface VisualPartEntityRenderState {
    int kltyton_bedrock_runtime$getEntityId();

    void kltyton_bedrock_runtime$setEntityId(int entityId);
}
