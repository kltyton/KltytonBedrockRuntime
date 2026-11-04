package com.kltyton.bedrockruntime.client.particle.model.loading;

import com.kltyton.bedrockruntime.client.particle.model.bake.NativeGeometryParticleBakery;
import com.kltyton.bedrockruntime.client.particle.model.geometry.GeometryParticleSpriteSource;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleLoader;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.LinkedHashSet;

public final class NeoForgeGeometryModelLoading {
    private NeoForgeGeometryModelLoading() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeGeometryModelLoading::registerSpriteSources);
        modBus.addListener(NeoForgeGeometryModelLoading::registerStandalone);
    }

    private static void registerSpriteSources(RegisterSpriteSourcesEvent event) {
        event.register(BedrockParticles.asResource("geometry_particles"), GeometryParticleSpriteSource.CODEC);
    }

    private static void registerStandalone(ModelEvent.RegisterStandalone event) {
        NativeGeometryParticleBakery.clear();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        for (String modelId : new LinkedHashSet<>(ModelParticleLoader.geometryModelIds(resources).values())) {
            Identifier id = Identifier.tryParse(modelId);
            if (id == null) {
                continue;
            }
            StandaloneModelKey<QuadCollection> key = new StandaloneModelKey<>(id::toString);
            event.register(key, new SimpleUnbakedStandaloneModel<>(id, (resolved, baker, name) -> {
                QuadCollection quads = resolved.bakeTopGeometry(resolved.getTopTextureSlots(),
                        baker, BlockModelRotation.IDENTITY);
                NativeGeometryParticleBakery.publishQuads(id.toString(), quads.getAll());
                return quads;
            }));
        }
    }
}
