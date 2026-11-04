package com.kltyton.bedrockruntime.client.particle.model.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * One animation frame of {@code model_animation.type_frames}. A frame may contain several weighted
 * model choices, matching ThreeDimensionParticle's weighted frame semantics.
 */
public record ModelParticleFrame(List<Entry> entries) {
    public ModelParticleFrame {
        entries = List.copyOf(entries == null ? List.of() : entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("model particle frame must contain at least one model");
        }
    }

    public ModelParticleFrame(String model) {
        this(List.of(new Entry(model, 1)));
    }

    public ModelParticleFrame(String model, int weight) {
        this(List.of(new Entry(model, weight)));
    }

    public String select(double random) {
        if (entries.size() == 1) {
            return entries.get(0).model();
        }
        if (!Double.isFinite(random)) {
            throw new IllegalArgumentException("model particle random value must be finite");
        }
        long total = 0;
        for (Entry entry : entries) {
            total += Math.max(1, entry.weight());
        }
        double normalized = random - Math.floor(random);
        long target = Math.min(total - 1L, (long) (normalized * total));
        long cursor = 0;
        for (Entry entry : entries) {
            cursor += Math.max(1, entry.weight());
            if (target < cursor) {
                return entry.model();
            }
        }
        return entries.get(entries.size() - 1).model();
    }

    public String firstModel() {
        return entries.get(0).model();
    }

    public static ModelParticleFrame parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("model particle frame must not be null");
        }
        if (element.isJsonPrimitive()) {
            return new ModelParticleFrame(element.getAsString());
        }
        if (element.isJsonArray()) {
            java.util.ArrayList<Entry> entries = new java.util.ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                entries.add(parseEntry(child));
            }
            return new ModelParticleFrame(entries);
        }
        if (element.isJsonObject()) {
            return new ModelParticleFrame(List.of(parseEntry(element)));
        }
        throw new IllegalArgumentException("model particle frame must be a string, object or array");
    }

    private static Entry parseEntry(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("model particle frame entry must not be null");
        }
        if (element.isJsonPrimitive()) {
            return new Entry(element.getAsString(), 1);
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("model particle frame entry must be a string or object");
        }
        JsonObject object = element.getAsJsonObject();
        String model = firstString(object, "model_type", "model", "model_id");
        if (model == null) {
            throw new IllegalArgumentException("model particle frame entry is missing model_type/model/model_id");
        }
        int weight = object.has("weight") && object.get("weight").isJsonPrimitive()
                ? object.get("weight").getAsInt() : 1;
        return new Entry(model, weight);
    }

    private static String firstString(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && object.get(name).isJsonPrimitive()) {
                String value = object.get(name).getAsString();
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    public record Entry(String model, int weight) {
        public Entry {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model particle frame entry must name a model");
            }
            model = model.trim();
            if (weight <= 0) {
                weight = 1;
            }
        }
    }

    @Override
    public String toString() {
        return entries.size() == 1 ? entries.get(0).model() : entries.toString();
    }
}
