package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleResources;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Links particle scripts to the language version of their source animation pack. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.FileLoader", remap = false)
public abstract class GeckoLibFileLoaderMixin {
    @WrapMethod(method = "loadAnimationsFile")
    private static BakedAnimations kltyton_bedrock_runtime$preserveScriptVersion(ResourceLocation id, ResourceManager resources,
                                                                       Operation<BakedAnimations> original) {
        BakedAnimations baked = original.call(id, resources);
        if (baked == null) return null;
        MolangVersion version;
        boolean bedrockItemNames;
        try {
            Resource resource = resources.getResourceOrThrow(id);
            version = BedrockParticleResources.readVersion(resource);
            try (var reader = resource.openAsReader()) {
                var document = JsonParser.parseReader(reader).getAsJsonObject();
                bedrockItemNames = BedrockParticleResources.usesBedrockItemNames(document,
                        resource.source() instanceof BedrockPackResources);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read animation pack Molang version: " + id, exception);
        }
        for (var animation : baked.animations().values()) {
            for (var particle : animation.keyFrames().particles()) {
                IPSParticleKeyframeData data = IPSParticleKeyframeData.of(particle);
                data.kltyton_bedrock_runtime$setMolangVersion(version);
                data.kltyton_bedrock_runtime$setBedrockItemNames(bedrockItemNames);
            }
        }
        return baked;
    }
}

