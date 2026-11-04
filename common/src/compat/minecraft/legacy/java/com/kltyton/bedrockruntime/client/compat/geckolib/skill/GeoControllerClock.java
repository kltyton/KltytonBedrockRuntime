package com.kltyton.bedrockruntime.client.compat.geckolib.skill;

/** Read-only access to the clock that GeckoLib actually processed. */
public interface GeoControllerClock {
    double kltyton_bedrock_runtime$getTimelineTick();
    double kltyton_bedrock_runtime$getTickOffset();
}
