package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.access.IPSGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.cache.object.GeoBone", remap = false)
public abstract class GeoBoneMixin implements IPSGeoBone {
    @Unique
    private @Nullable Map<String, LocatorValue> kltyton_bedrock_runtime$locators;

    @Override
    public @Nullable Map<String, LocatorValue> kltyton_bedrock_runtime$getLocators() {
        return kltyton_bedrock_runtime$locators;
    }

    @Override
    public void kltyton_bedrock_runtime$setLocators(@Nullable Map<String, LocatorValue> locators) {
        this.kltyton_bedrock_runtime$locators = locators;
    }
}
