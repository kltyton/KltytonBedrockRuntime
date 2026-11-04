package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.google.gson.JsonParser;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
        MolangVersion version = null;
        boolean bedrockItemNames = false;
        for (var animation : baked.animations().values()) {
            for (var particle : animation.keyFrames().particles()) {
                if (version == null) {
                    try {
                        var resource = resources.getResourceOrThrow(id);
                        version = BedrockParticleResources.readVersion(resource);
                        try (var reader = resource.openAsReader()) {
                            var document = JsonParser.parseReader(reader).getAsJsonObject();
                            bedrockItemNames = BedrockParticleResources.usesBedrockItemNames(document,
                                    resource.source() instanceof BedrockPackResources);
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException("Cannot read animation particle script format: " + id, exception);
                    }
                }
                IPSParticleKeyframeData.of(particle).kltyton_bedrock_runtime$setMolangVersion(version);
                IPSParticleKeyframeData.of(particle).kltyton_bedrock_runtime$setBedrockItemNames(bedrockItemNames);
            }
        }
        return baked;
    }
}

