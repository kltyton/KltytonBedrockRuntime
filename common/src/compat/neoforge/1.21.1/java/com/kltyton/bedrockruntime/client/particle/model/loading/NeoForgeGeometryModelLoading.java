package com.kltyton.bedrockruntime.client.particle.model.loading;

import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.geometry.GeometryParticleSpriteSource;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleLoader;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeGeometryModelLoading {
    private static final Map<ModelResourceLocation, String> REGISTERED = new ConcurrentHashMap<>();

    private NeoForgeGeometryModelLoading() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeGeometryModelLoading::registerSpriteSourceTypes);
        modBus.addListener(NeoForgeGeometryModelLoading::registerAdditional);
        modBus.addListener(NeoForgeGeometryModelLoading::modifyBakingResult);
    }

    private static void registerSpriteSourceTypes(RegisterSpriteSourceTypesEvent event) {
        SpriteSourceType sourceType = event.register(BedrockParticles.asResource("geometry_particles"),
                GeometryParticleSpriteSource.codec());
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
            ModelResourceLocation modelLocation = ModelResourceLocation.standalone(location);
            REGISTERED.put(modelLocation, location.toString());
            event.register(modelLocation);
        }
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ModelResourceLocation, String> entry : REGISTERED.entrySet()) {
            BakedModel model = event.getModels().get(entry.getKey());
            if (model != null) {
                NativeGeometryParticleBakery.publishFromModel(entry.getValue(), model);
            }
        }
    }
}
