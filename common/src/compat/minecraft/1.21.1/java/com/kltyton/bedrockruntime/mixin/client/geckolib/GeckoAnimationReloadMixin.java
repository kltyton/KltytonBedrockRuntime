package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** Joins GeckoLib 4.9.2's asynchronous animation bake before its reload publishes the cache. */
@Mixin(value = GeckoLibCache.class, remap = false)
public abstract class GeckoAnimationReloadMixin {
    @Unique
    private static final ThreadLocal<Consumer<CompletableFuture<Void>>> kltyton_bedrock_runtime$animationBake = new ThreadLocal<>();

    @WrapOperation(method = "loadAnimations", at = @At(value = "INVOKE", ordinal = 0,
            target = "Ljava/util/concurrent/CompletableFuture;thenRunAsync(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<Void> kltyton_bedrock_runtime$awaitAnimationBake(
            CompletableFuture<?> stage, Runnable load, Operation<CompletableFuture<Void>> original) {
        var bake = new AtomicReference<>(CompletableFuture.<Void>completedFuture(null));
        Runnable loadAndCapture = () -> {
            var previous = kltyton_bedrock_runtime$animationBake.get();
            kltyton_bedrock_runtime$animationBake.set(bake::set);
            try {
                load.run();
            } finally {
                if (previous == null) kltyton_bedrock_runtime$animationBake.remove();
                else kltyton_bedrock_runtime$animationBake.set(previous);
            }
        };
        return original.call(stage, loadAndCapture).thenCompose(ignored -> bake.get());
    }

    @WrapMethod(method = "loadResources")
    private static <T> CompletableFuture<Void> kltyton_bedrock_runtime$captureAnimationBake(
            Executor executor, ResourceManager resources, String directory, Function<ResourceLocation, T> loader,
            BiConsumer<ResourceLocation, T> accept, Operation<CompletableFuture<Void>> original) {
        var result = original.call(executor, resources, directory, loader, accept);
        var capture = kltyton_bedrock_runtime$animationBake.get();
        if (capture != null && directory.equals("animations")) capture.accept(result);
        return result;
    }
}
