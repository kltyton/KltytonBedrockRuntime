package com.kltyton.bedrockruntime.client.compat.geckolib.loading;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.util.GeckoLibUtil;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.EnhancedBakedGeoModel;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resource opt-in for extended .geo.json models; animation loading remains GeckoLib's. */
public final class EnhancedGeometryLoader extends GeckoLibGsonLoader {
    private static final Set<String> RESOURCES = ConcurrentHashMap.newKeySet();
    private static boolean registered;

    public static void registerResource(String resource) {
        RESOURCES.add(resource);
    }

    public static synchronized void register() {
        if (!registered) {
            EnhancedModels.installClientGraphResolver(EnhancedGeometryLoader::graph);
            GeckoLibUtil.addResourceLoader((id, resource) -> accepts(id), new EnhancedGeometryLoader());
            registered = true;
        }
    }

    private static boolean accepts(Identifier id) {
        String path = id.getPath();
        return path.endsWith(".geo.json") && (RESOURCES.contains(id.toString())
                || com.kltyton.bedrockruntime.api.model.EnhancedModels.isRegistered(id.toString())
                || path.startsWith("kltyton_bedrock_runtime/") || path.contains("/kltyton_bedrock_runtime/"));
    }

    @Override
    public BakedGeoModel bakeGeckoLibModelFile(Identifier resource, JsonObject json) {
        EnhancedModelData data = EnhancedModelData.parse(resource.toString(), json);
        // GeckoLib 5's intermediate polygon schema cannot represent all Bedrock polygons.
        // Keep those in the extension; its native baker continues to own cubes and locators.
        JsonObject cubeGeometry = json.deepCopy();
        for (var definition : cubeGeometry.getAsJsonArray("minecraft:geometry")) {
            var object = definition.getAsJsonObject();
            if (object.has("bones")) {
                for (var bone : object.getAsJsonArray("bones")) {
                    bone.getAsJsonObject().remove("poly_mesh");
                }
            }
        }
        BakedGeoModel original = super.bakeGeckoLibModelFile(resource, cubeGeometry);
        return new EnhancedBakedGeoModel(original, data);
    }

    private static GraphDefinition graph(String resource) {
        Identifier id = Identifier.tryParse(resource);
        if (id == null) {
            throw new IllegalArgumentException("Invalid graph model resource: " + resource);
        }
        BakedGeoModel model = GeckoLibResources.getBakedModels().getModel(GeckoLibResources.stripPrefixAndSuffix(id));
        if (!(model instanceof EnhancedBakedGeoModel enhanced)) {
            throw new IllegalStateException(resource + ": graph model has not been baked with the enhanced loader");
        }
        return enhanced.extension().graph().orElseThrow(
                () -> new IllegalArgumentException(resource + ": missing kltyton_bedrock_runtime.graph"));
    }
}
