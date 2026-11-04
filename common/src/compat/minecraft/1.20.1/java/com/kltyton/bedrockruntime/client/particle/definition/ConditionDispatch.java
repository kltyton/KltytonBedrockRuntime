package com.kltyton.bedrockruntime.client.particle.definition;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.client.particle.api.IComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.BoolMolangExp;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record ConditionDispatch<T>(BoolMolangExp condition, T value) {
    public static <T> Codec<ConditionDispatch<T>> codec(Map<ResourceLocation, Codec<? extends T>> registry) {
        return codec(registry, value -> {
            if (!(value instanceof IComponent component)) {
                throw new IllegalArgumentException("A type identifier resolver is required for " + value.getClass().getName());
            }
            Codec<?> componentCodec = component.codec();
            List<ResourceLocation> matches = registry.entrySet().stream()
                    .filter(entry -> entry.getValue() == componentCodec).map(Map.Entry::getKey).toList();
            if (matches.size() != 1) throw new IllegalArgumentException("Expected one registered codec for " + value.getClass().getName());
            return matches.get(0);
        });
    }

    public static <T> Codec<ConditionDispatch<T>> codec(Map<ResourceLocation, Codec<? extends T>> registry,
                                                        Function<? super T, ResourceLocation> typeId) {
        return new ConditionDispatchCodec<>(registry, typeId).codec();
    }

    private static final class ConditionDispatchCodec<T> extends MapCodec<ConditionDispatch<T>> {
        private static final String CONDITION_KEY = "condition";
        private final Map<ResourceLocation, Codec<? extends T>> registry;
        private final Function<? super T, ResourceLocation> typeId;

        ConditionDispatchCodec(Map<ResourceLocation, Codec<? extends T>> registry, Function<? super T, ResourceLocation> typeId) {
            this.registry = registry;
            this.typeId = typeId;
        }

        @Override
        public <U> DataResult<ConditionDispatch<T>> decode(DynamicOps<U> ops, MapLike<U> input) {
            U conditionValue = input.get(CONDITION_KEY);
            if (conditionValue == null) {
                return DataResult.error(() -> "Missing required field: 'condition'");
            }
            BoolMolangExp condition = BoolMolangExp.CODEC.parse(ops, conditionValue).getOrThrow(false, msg -> { throw new IllegalStateException(msg); });
            List<Pair<U, U>> typeEntries = input.entries().filter(pair -> {
                String key = ops.getStringValue(pair.getFirst()).result().orElse("");
                return !key.equals(CONDITION_KEY);
            }).toList();

            if (typeEntries.size() != 1) {
                return DataResult.error(() -> "Expected exactly one type field, found " + typeEntries.size());
            }

            U typeKey = typeEntries.get(0).getFirst();
            U typeValue = typeEntries.get(0).getSecond();
            ResourceLocation typeId;
            try {
                String idStr = ops.getStringValue(typeKey).getOrThrow(false, msg -> { throw new IllegalStateException(msg); });
                typeId = new ResourceLocation(idStr);
            } catch (Exception e) {
                return DataResult.error(() -> "Invalid type id: " + e.getMessage());
            }
            Codec<? extends T> valueCodec = registry.get(typeId);
            if (valueCodec == null) {
                return DataResult.error(() -> "Unknown type: " + typeId);
            }
            T value = valueCodec.parse(ops, typeValue).getOrThrow(false, msg -> { throw new IllegalStateException(msg); });
            return DataResult.success(new ConditionDispatch<>(condition, value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> RecordBuilder<U> encode(ConditionDispatch<T> input, DynamicOps<U> ops, RecordBuilder<U> prefix) {
            RecordBuilder<U> result = BoolMolangExp.CODEC.fieldOf(CONDITION_KEY).encode(input.condition(), ops, prefix);
            ResourceLocation id = typeId.apply(input.value());
            Codec<T> valueCodec = (Codec<T>) registry.get(id);
            if (valueCodec == null) return result.withErrorsFrom(DataResult.error(() -> "Unknown type: " + id));
            return result.add(id.toString(), valueCodec.encodeStart(ops, input.value()));
        }

        @Override
        public <U> Stream<U> keys(DynamicOps<U> ops) {
            return Stream.concat(
                    Stream.of(ops.createString(CONDITION_KEY)),
                    registry.keySet().stream().map(id -> ops.createString(id.toString()))
            );
        }
    }
}
