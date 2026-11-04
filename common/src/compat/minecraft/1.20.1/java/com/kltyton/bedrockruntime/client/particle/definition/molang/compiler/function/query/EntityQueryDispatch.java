package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;

public final class EntityQueryDispatch implements MathValue {
    private final Map<EntityType<?>, Function<List<MathValue>, MathValue>> factories;
    private final Map<EntityType<?>, MathValue> expressions = new ConcurrentHashMap<>();
    private final List<MathValue> arguments;
    private final Supplier<MathValue> fallbackFactory;
    private volatile boolean fallbackInitialized;
    private @Nullable MathValue fallback;

    public EntityQueryDispatch(Map<EntityType<?>, Function<List<MathValue>, MathValue>> factories,
                               List<MathValue> arguments, Supplier<MathValue> fallbackFactory) {
        this.factories = Map.copyOf(factories);
        this.arguments = List.copyOf(arguments);
        this.fallbackFactory = fallbackFactory;
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        Entity entity = instance.getAttachedEntity();
        Function<List<MathValue>, MathValue> factory = entity == null ? null : factories.get(entity.getType());
        MathValue expression = factory == null ? fallback() : expressions.computeIfAbsent(entity.getType(),
                ignored -> Objects.requireNonNull(factory.apply(arguments), "Entity query factory returned null"));
        return expression == null ? 0.0 : MathValue.resolveRaw(expression, instance);
    }

    @Override
    public double get(MolangInstance instance) {
        Object value = getRaw(instance);
        return value instanceof Number number ? number.floatValue() : 0.0;
    }

    private @Nullable MathValue fallback() {
        if (!fallbackInitialized) {
            synchronized (this) {
                if (!fallbackInitialized) {
                    fallback = fallbackFactory.get();
                    fallbackInitialized = true;
                }
            }
        }
        return fallback;
    }

    @Override
    public void markImmutable() {
        arguments.forEach(MathValue::markImmutable);
    }
}
