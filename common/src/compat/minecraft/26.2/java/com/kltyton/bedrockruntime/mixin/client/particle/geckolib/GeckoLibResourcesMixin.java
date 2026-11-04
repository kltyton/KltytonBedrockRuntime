package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.loading.loader.GeckoLibLoader;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.function.TriFunction;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockParticleResources;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackResources;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** Carries each animation pack's Molang version through its loader callbacks. */
@Pseudo
@Mixin(targets = "com.geckolib.cache.GeckoLibResources", remap = false)
public abstract class GeckoLibResourcesMixin {
    @WrapOperation(method = "loadAnimations", at = @At(value = "INVOKE",
            target = "Lcom/geckolib/cache/GeckoLibResources;bakeAllResources(Ljava/util/concurrent/Executor;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/lang/String;Lorg/apache/commons/lang3/function/TriFunction;Lorg/apache/commons/lang3/function/TriFunction;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<Map<Identifier, BakedAnimations>> kltyton_bedrock_runtime$preserveScriptVersions(
            Executor executor, ResourceManager resources, String rootPath,
            TriFunction<GeckoLibLoader<Object>, Identifier, Resource, Object> deserializer,
            TriFunction<GeckoLibLoader<Object>, Identifier, Object, BakedAnimations> bakery,
            Supplier<BakedAnimations> onException,
            Operation<CompletableFuture<Map<Identifier, BakedAnimations>>> original) {
        ConcurrentMap<Identifier, Resource> sources = new ConcurrentHashMap<>();
        TriFunction<GeckoLibLoader<Object>, Identifier, Resource, Object> versionedDeserializer = (loader, id, resource) -> {
            Object raw = deserializer.apply(loader, id, resource);
            sources.put(id, resource);
            return raw;
        };
        TriFunction<GeckoLibLoader<Object>, Identifier, Object, BakedAnimations> versionedBakery = (loader, id, raw) -> {
            Resource source = sources.remove(id);
            JsonElement itemNameFormat = raw instanceof JsonObject json ? json.get(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID + ":item_name_format") : null;
            BakedAnimations baked = bakery.apply(loader, id, raw);
            if (baked != null) {
                MolangVersion version = null;
                boolean bedrockItemNames = false;
                for (var animation : baked.animations().values()) {
                    for (var particle : animation.keyframeMarkers().particles()) {
                        if (version == null) {
                            if (source == null) throw new IllegalStateException("Missing animation resource: " + id);
                            try {
                                version = BedrockParticleResources.readVersion(source);
                            } catch (IOException exception) {
                                throw new UncheckedIOException("Cannot read animation pack Molang version: " + id, exception);
                            }
                            bedrockItemNames = BedrockParticleResources.usesBedrockItemNames(itemNameFormat,
                                    source.source() instanceof BedrockPackResources);
                        }
                        IPSParticleKeyframeData.of(particle).kltyton_bedrock_runtime$setMolangVersion(version);
                        IPSParticleKeyframeData.of(particle).kltyton_bedrock_runtime$setBedrockItemNames(bedrockItemNames);
                    }
                }
            }
            return baked;
        };
        return original.call(executor, resources, rootPath, versionedDeserializer, versionedBakery, onException);
    }
}

