package com.kltyton.bedrockruntime.mixin.client.render;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerRenderer.class)
public interface PlayerRendererAccessor {
    @Invoker("setModelProperties")
    void kltyton_bedrock_runtime$setModelProperties(AbstractClientPlayer player);
}
