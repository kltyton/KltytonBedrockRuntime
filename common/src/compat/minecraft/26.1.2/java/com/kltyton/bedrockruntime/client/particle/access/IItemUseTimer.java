package com.kltyton.bedrockruntime.client.particle.access;

/** Supplies item use timing for particle queries. */
@org.jetbrains.annotations.ApiStatus.Internal
public interface IItemUseTimer {
    int kltyton_bedrock_runtime$itemUseRemaining(int javaRemaining);

    void kltyton_bedrock_runtime$setRemoteItemUseState(boolean active, int remaining);
}
