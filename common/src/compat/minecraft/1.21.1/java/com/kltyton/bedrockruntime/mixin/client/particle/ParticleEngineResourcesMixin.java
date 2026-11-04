package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import com.kltyton.bedrockruntime.client.particle.bedrock.JavaParticleResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineResourcesMixin {
    @Shadow @Final private Map<ResourceLocation, ?> spriteSets;

    @ModifyVariable(method = "reload", at = @At("HEAD"), argsOnly = true)
    private ResourceManager kltyton_bedrock_runtime$preserveJavaParticles(ResourceManager manager) {
        return new JavaParticleResourceManager(manager, spriteSets.keySet());
    }
}
