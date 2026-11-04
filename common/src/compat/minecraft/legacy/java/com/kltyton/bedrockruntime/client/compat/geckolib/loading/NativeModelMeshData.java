package com.kltyton.bedrockruntime.client.compat.geckolib.loading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.MeshGeoBone;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Bone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.raw.ModelProperties;
import software.bernie.geckolib.loading.json.raw.PolyMesh;
import software.bernie.geckolib.loading.json.raw.PolysUnion;
import software.bernie.geckolib.loading.object.BoneStructure;
import software.bernie.geckolib.loading.object.GeometryTree;

public final class NativeModelMeshData {
    private static final Cache<ModelProperties, EnhancedModelData> INPUTS = CacheBuilder.newBuilder().weakKeys().build();
    private static final JsonDeserializer<PolyMesh> POLY_MESH = (json, type, context) -> {
        if (!json.isJsonObject()) {
            throw new JsonParseException("poly_mesh must be an object");
        }
        JsonObject object = json.getAsJsonObject();
        return new PolyMesh(optionalBoolean(object, "normalized_uvs"), doubles(object, "normals"),
                polys(object.get("polys")), doubles(object, "positions"), doubles(object, "uvs"));
    };

    private NativeModelMeshData() {
    }

    public static Gson localGson(Gson base) {
        return base.newBuilder().registerTypeAdapter(PolyMesh.class, POLY_MESH).create();
    }

    public static void remember(Model model, EnhancedModelData data) {
        ModelProperties properties = model.minecraftGeometry()[0].modelProperties();
        if (properties == null) {
            throw new IllegalArgumentException(data.resource() + ": enhanced model is missing its description");
        }
        INPUTS.put(properties, data);
    }

    public static EnhancedModelData require(ModelProperties properties, String resource) {
        EnhancedModelData data = INPUTS.getIfPresent(properties);
        if (data == null) {
            throw new IllegalStateException(resource + ": enhanced geometry was not captured by the resource reader");
        }
        return data;
    }

    public static void attach(BakedGeoModel model, GeometryTree tree, String resource,
                              EnhancedModelData data, boolean doubleSided) {
        Map<String, Bone> raw = new LinkedHashMap<>();
        for (BoneStructure structure : tree.topLevelBones().values()) {
            collectRaw(structure, raw);
        }
        MeshGeoBone.attach(model, data, resource, doubleSided, raw);
    }

    private static void collectRaw(BoneStructure structure, Map<String, Bone> out) {
        Bone bone = structure.self();
        if (bone != null && bone.name() != null) {
            out.put(bone.name(), bone);
        }
        for (BoneStructure child : structure.children().values()) {
            collectRaw(child, out);
        }
    }

    private static Boolean optionalBoolean(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("poly_mesh." + name + " must be a boolean");
        }
        return element.getAsBoolean();
    }

    private static double[] doubles(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return new double[3];
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("poly_mesh." + name + " must be an array");
        }
        return flatten(element.getAsJsonArray());
    }

    private static double[] flatten(JsonArray array) {
        List<Double> values = new ArrayList<>();
        append(array, values);
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static void append(JsonElement element, List<Double> out) {
        if (element == null || element.isJsonNull()) {
            throw new JsonParseException("poly_mesh contains null row entries");
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                append(child, out);
            }
            return;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("poly_mesh row entries must be numbers");
        }
        out.add(element.getAsDouble());
    }

    private static PolysUnion polys(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String mode = element.getAsString();
            PolysUnion.Type type;
            if ("tri_list".equals(mode)) {
                type = PolysUnion.Type.TRI;
            } else if ("quad_list".equals(mode)) {
                type = PolysUnion.Type.QUAD;
            } else {
                throw new JsonParseException("Unsupported poly_mesh mode: " + mode);
            }
            return new PolysUnion(new double[0][0][0], type);
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("poly_mesh.polys must be a string or array");
        }
        JsonArray polygons = element.getAsJsonArray();
        double[][][] union = new double[polygons.size()][][];
        for (int polygonIndex = 0; polygonIndex < polygons.size(); polygonIndex++) {
            JsonElement polygonElement = polygons.get(polygonIndex);
            if (polygonElement == null || !polygonElement.isJsonArray()) {
                throw new JsonParseException("poly_mesh polygon " + polygonIndex + " must be an array");
            }
            JsonArray polygon = polygonElement.getAsJsonArray();
            double[][] rows = new double[polygon.size()][];
            for (int vertexIndex = 0; vertexIndex < polygon.size(); vertexIndex++) {
                JsonElement vertex = polygon.get(vertexIndex);
                if (vertex != null && vertex.isJsonArray()) {
                    rows[vertexIndex] = flatten(vertex.getAsJsonArray());
                } else if (vertex != null && vertex.isJsonPrimitive()
                        && vertex.getAsJsonPrimitive().isNumber()) {
                    rows[vertexIndex] = new double[]{vertex.getAsDouble()};
                } else {
                    throw new JsonParseException("poly_mesh polygon " + polygonIndex + " vertex "
                            + vertexIndex + " must be an array of numbers");
                }
            }
            union[polygonIndex] = rows;
        }
        return new PolysUnion(union, null);
    }
}
