package com.kltyton.bedrockruntime.client.particle.access;

import org.jetbrains.annotations.Nullable;

/** Retains the lookup key for a named stage resolved by GeckoLib's animation processor. */
public interface IPSQueuedAnimation {
    @Nullable String kltyton_bedrock_runtime$getStageName();

    void kltyton_bedrock_runtime$setStageName(@Nullable String name);
}
