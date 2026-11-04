package com.kltyton.bedrockruntime.api.state.runtime;

import org.jetbrains.annotations.Nullable;

/** Internal entity attachment supplied by the common Entity mixin. */
public interface ObbEntityStateAccess {
    @Nullable ObbEntityState kltyton_bedrock_runtime$getAttachedState();
    void kltyton_bedrock_runtime$setAttachedState(ObbEntityState state);
}
