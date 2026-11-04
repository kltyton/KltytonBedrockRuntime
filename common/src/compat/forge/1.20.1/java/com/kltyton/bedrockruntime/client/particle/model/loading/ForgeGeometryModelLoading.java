package com.kltyton.bedrockruntime.client.particle.model.loading;

import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.geometry.GeometryParticleSpriteSource;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleLoader;
import com.kltyton.bedrockruntime.mixin.client.particle.SpriteSourcesAccessor;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeGeometryModelLoading {
    private static final Set<ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();

    private ForgeGeometryModelLoading() {
    }

    public static void register(IEventBus modBus) {
        registerSpriteSource();
        modBus.addListener(ForgeGeometryModelLoading::registerAdditional);
        modBus.addListener(ForgeGeometryModelLoading::modifyBakingResult);
    }

    private static void registerSpriteSource() {
        SpriteSourceType sourceType = new SpriteSourceType(GeometryParticleSpriteSource.codec());
        if (SpriteSourcesAccessor.kltyton_bedrock_runtime$getTypes()
                .putIfAbsent(BedrockParticles.asResource("geometry_particles"), sourceType) != null) {
            throw new IllegalStateException("Duplicate geometry particle sprite source registration");
        }
        GeometryParticleSpriteSource.bindType(sourceType);
    }

    private static void registerAdditional(ModelEvent.RegisterAdditional event) {
        NativeGeometryParticleBakery.clear();
        REGISTERED.clear();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        for (String modelId : new LinkedHashSet<>(ModelParticleLoader.geometryModelIds(resources).values())) {
            ResourceLocation location = ResourceLocation.tryParse(modelId);
            if (location == null) {
                continue;
            }
            REGISTERED.add(location);
            event.register(location);
        }
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (ResourceLocation id : REGISTERED) {
            BakedModel model = event.getModels().get(id);
            if (model != null) {
                NativeGeometryParticleBakery.publishFromModel(id.toString(), model);
            }
        }
    }
}
