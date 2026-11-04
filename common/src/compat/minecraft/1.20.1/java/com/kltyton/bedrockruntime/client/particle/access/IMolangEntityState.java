package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

public interface IMolangEntityState {
    boolean kltyton_bedrock_runtime$targetStateKnown();

    boolean kltyton_bedrock_runtime$hasTarget();

    boolean kltyton_bedrock_runtime$loveStateKnown();

    boolean kltyton_bedrock_runtime$isInLove();

    boolean kltyton_bedrock_runtime$recoveryStateKnown();

    @Nullable GlobalPos kltyton_bedrock_runtime$recoveryTarget();

    void kltyton_bedrock_runtime$setMolangEntityState(int flags, @Nullable GlobalPos recoveryTarget);
}
