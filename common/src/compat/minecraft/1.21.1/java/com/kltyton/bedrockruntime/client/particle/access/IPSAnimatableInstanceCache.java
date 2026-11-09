package com.kltyton.bedrockruntime.client.particle.access;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IPSAnimatableInstanceCache {
    Object2ObjectMap<LocatorValue, IntList> kltyton_bedrock_runtime$getCachedId();

    void kltyton_bedrock_runtime$createLocatorState(LocatorValue locator);

    @Nullable GeckoLibHelper.LocatorState kltyton_bedrock_runtime$getLocatorState(LocatorValue locator);

    void kltyton_bedrock_runtime$clearLocatorCache();

    static IPSAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IPSAnimatableInstanceCache) cache;
    }
}
