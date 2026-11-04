package com.kltyton.bedrockruntime.mixin.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Registers the custom Fabric particle pipelines in the native precompilation registry. */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesInvoker {
    @Invoker("register")
    static RenderPipeline kltyton_bedrock_runtime$register(RenderPipeline pipeline) {
        throw new AssertionError("Mixin invoker was not transformed");
    }
}
