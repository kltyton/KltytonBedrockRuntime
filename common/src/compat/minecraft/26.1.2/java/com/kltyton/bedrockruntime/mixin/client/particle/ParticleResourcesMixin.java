package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import com.kltyton.bedrockruntime.client.particle.bedrock.JavaParticleResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/** Preserves Java sprite definitions when a Bedrock file uses the same resource path. */
@Mixin(ParticleResources.class)
public abstract class ParticleResourcesMixin {
    @Shadow @Final private Map<Identifier, ?> spriteSets;

    @ModifyExpressionValue(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;resourceManager()Lnet/minecraft/server/packs/resources/ResourceManager;"))
    private ResourceManager kltyton_bedrock_runtime$preserveJavaParticles(ResourceManager manager) {
        return new JavaParticleResourceManager(manager, spriteSets.keySet());
    }
}
