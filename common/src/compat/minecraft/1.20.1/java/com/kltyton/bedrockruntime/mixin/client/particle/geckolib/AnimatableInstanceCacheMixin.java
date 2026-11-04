package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.HashMap;
import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache", remap = false)
public abstract class AnimatableInstanceCacheMixin implements IPSAnimatableInstanceCache {
    @Unique
    private final Map<LocatorValue, IntArrayList> kltyton_bedrock_runtime$cachedIds = new HashMap<>();
    @Unique
    private final Map<LocatorValue, GeckoLibHelper.LocatorState> kltyton_bedrock_runtime$locatorStates = new HashMap<>();

    @Override
    public Map<LocatorValue, IntArrayList> kltyton_bedrock_runtime$getCachedId() {
        return kltyton_bedrock_runtime$cachedIds;
    }

    @Override
    public void kltyton_bedrock_runtime$createLocatorState(LocatorValue locator) {
        kltyton_bedrock_runtime$locatorStates.put(locator, new GeckoLibHelper.LocatorState(locator));
    }

    @Override
    public @Nullable GeckoLibHelper.LocatorState kltyton_bedrock_runtime$getLocatorState(LocatorValue locator) {
        return kltyton_bedrock_runtime$locatorStates.get(locator);
    }
}
