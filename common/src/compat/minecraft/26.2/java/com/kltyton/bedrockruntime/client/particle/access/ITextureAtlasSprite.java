package com.kltyton.bedrockruntime.client.particle.access;

public interface ITextureAtlasSprite extends IPSTextureAtlasSprite {
    int kltyton_bedrock_runtime$getOriginX();

    int kltyton_bedrock_runtime$getOriginY();

    @Override default float kltyton_bedrock_runtime$getInvOx() { return 1.0F / kltyton_bedrock_runtime$getOriginX(); }
    @Override default float kltyton_bedrock_runtime$getInvOy() { return 1.0F / kltyton_bedrock_runtime$getOriginY(); }
}
