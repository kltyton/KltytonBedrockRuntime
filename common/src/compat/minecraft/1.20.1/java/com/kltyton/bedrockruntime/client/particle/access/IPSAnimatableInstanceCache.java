package com.kltyton.bedrockruntime.client.particle.access;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IPSAnimatableInstanceCache {
    Map<LocatorValue, IntArrayList> kltyton_bedrock_runtime$getCachedId();

    void kltyton_bedrock_runtime$createLocatorState(LocatorValue locator);

    @Nullable GeckoLibHelper.LocatorState kltyton_bedrock_runtime$getLocatorState(LocatorValue locator);

    static IPSAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IPSAnimatableInstanceCache) cache;
    }
}
