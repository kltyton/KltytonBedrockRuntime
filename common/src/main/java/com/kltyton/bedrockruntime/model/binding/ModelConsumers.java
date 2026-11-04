package com.kltyton.bedrockruntime.model.binding;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ModelConsumers {
    private final Map<ModelConsumer, Set<String>> references;

    private ModelConsumers(Map<ModelConsumer, Set<String>> references) {
        this.references = references;
    }

    public static ModelConsumers empty() {
        return new ModelConsumers(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean references(String boneName) {
        if (boneName == null) {
            return false;
        }
        for (Set<String> bones : references.values()) {
            if (bones.contains(boneName)) {
                return true;
            }
        }
        return false;
    }

    public boolean references(ModelConsumer consumer, String boneName) {
        Set<String> bones = references.get(consumer);
        return bones != null && bones.contains(boneName);
    }

    public Set<String> bones(ModelConsumer consumer) {
        Set<String> bones = references.get(consumer);
        return bones == null ? Set.of() : bones;
    }

    public Set<ModelConsumer> activeConsumers() {
        return references.keySet();
    }

    public Set<String> allReferencedBones() {
        Set<String> result = new LinkedHashSet<>();
        for (Set<String> bones : references.values()) {
            result.addAll(bones);
        }
        return Collections.unmodifiableSet(result);
    }

    public boolean isEmpty() {
        return references.isEmpty();
    }

    public static final class Builder {
        private final Map<ModelConsumer, Set<String>> references = new EnumMap<>(ModelConsumer.class);

        private Builder() {
        }

        public Builder add(ModelConsumer consumer, String boneName) {
            Objects.requireNonNull(consumer, "consumer");
            Objects.requireNonNull(boneName, "boneName");
            references.computeIfAbsent(consumer, ignored -> new LinkedHashSet<>()).add(boneName);
            return this;
        }

        public Builder addAll(ModelConsumer consumer, Iterable<String> boneNames) {
            Objects.requireNonNull(consumer, "consumer");
            Objects.requireNonNull(boneNames, "boneNames");
            Set<String> target = references.computeIfAbsent(consumer, ignored -> new LinkedHashSet<>());
            for (String boneName : boneNames) {
                target.add(Objects.requireNonNull(boneName, "boneName"));
            }
            return this;
        }

        public ModelConsumers build() {
            Map<ModelConsumer, Set<String>> copy = new EnumMap<>(ModelConsumer.class);
            for (Map.Entry<ModelConsumer, Set<String>> entry : references.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
                }
            }
            return new ModelConsumers(Collections.unmodifiableMap(copy));
        }
    }
}
