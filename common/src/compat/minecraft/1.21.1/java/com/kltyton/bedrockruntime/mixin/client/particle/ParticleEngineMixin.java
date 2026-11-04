package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.client.particle.runtime.render.ParticleRenderLayers;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow @Final @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Shadow @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void kltyton_bedrock_runtime$registerRenderTypes(CallbackInfo ci) {
        RENDER_ORDER = com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes.renderOrder(RENDER_ORDER);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/particle/ParticleEngine;RENDER_ORDER:Ljava/util/List;"))
    private List<ParticleRenderType> kltyton_bedrock_runtime$includeRegisteredRenderTypes(List<ParticleRenderType> original) {
        LinkedHashSet<ParticleRenderType> types = null;
        for (ParticleRenderType type : particles.keySet()) {
            if (type != ParticleRenderType.NO_RENDER && !original.contains(type)) {
                if (types == null) types = new LinkedHashSet<>(original);
                types.add(type);
            }
        }
        return types == null ? original : List.copyOf(types);
    }

    @Mixin(TextureAtlas.class)
    public static abstract class AtlasUploadMixin {
        @Shadow @Final
        private ResourceLocation location;

        @Inject(method = "upload", at = @At("TAIL"))
        private void kltyton_bedrock_runtime$rebindParticleSprites(CallbackInfo ci) {
            TextureAtlas atlas = (TextureAtlas) (Object) this;
            com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource.atlasStitched(atlas);
            com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource.atlasStitched(atlas);
            com.kltyton.bedrockruntime.client.particle.atlas.ParticleAtlasRevision.stitched();
            if (TextureAtlas.LOCATION_PARTICLES.equals(location)) RegisterCustomParticleTypeEvent.rebindSprites();
        }
    }
}
