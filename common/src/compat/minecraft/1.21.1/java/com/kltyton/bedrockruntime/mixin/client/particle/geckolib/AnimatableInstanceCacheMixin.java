package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.access.IPSAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animatable.instance.AnimatableInstanceCache", remap = false)
public abstract class AnimatableInstanceCacheMixin implements IPSAnimatableInstanceCache {
    @Unique
    private Object2ObjectMap<LocatorValue, IntList> kltyton_bedrock_runtime$cachedId;
    @Unique
    private Map<LocatorValue, GeckoLibHelper.LocatorState> kltyton_bedrock_runtime$transform;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addRunner(CallbackInfo ci) {
        GeckoLibHelper.addWeakReloadCallback(this, owner -> {
            if (owner.kltyton_bedrock_runtime$cachedId != null) owner.kltyton_bedrock_runtime$cachedId.clear();
            if (owner.kltyton_bedrock_runtime$transform != null) owner.kltyton_bedrock_runtime$transform.clear();
        });
    }

    @Override
    public Object2ObjectMap<LocatorValue, IntList> kltyton_bedrock_runtime$getCachedId() {
        if (kltyton_bedrock_runtime$cachedId == null) {
            this.kltyton_bedrock_runtime$cachedId = new Object2ObjectOpenHashMap<>();
        }
        return kltyton_bedrock_runtime$cachedId;
    }

    @Override
    public void kltyton_bedrock_runtime$createLocatorState(LocatorValue locator) {
        if (kltyton_bedrock_runtime$transform == null) {
            this.kltyton_bedrock_runtime$transform = new Object2ObjectOpenHashMap<>();
        }
        kltyton_bedrock_runtime$transform.put(locator, new GeckoLibHelper.LocatorState(locator));
    }

    @Override
    public @Nullable GeckoLibHelper.LocatorState kltyton_bedrock_runtime$getLocatorState(LocatorValue locator) {
        if (kltyton_bedrock_runtime$transform == null) {
            this.kltyton_bedrock_runtime$transform = new Object2ObjectOpenHashMap<>();
        }
        return kltyton_bedrock_runtime$transform.get(locator);
    }
}
