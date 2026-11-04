package com.kltyton.bedrockruntime.client.particle.access;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IPSGeoBone {
    @Nullable Map<String, LocatorValue> kltyton_bedrock_runtime$getLocators();

    void kltyton_bedrock_runtime$setLocators(@Nullable Map<String, LocatorValue> locators);

    static IPSGeoBone of(GeoBone geoBone) {
        return (IPSGeoBone) geoBone;
    }
}
