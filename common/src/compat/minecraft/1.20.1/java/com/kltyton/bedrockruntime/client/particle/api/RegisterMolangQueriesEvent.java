package com.kltyton.bedrockruntime.client.particle.api;


import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.List;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import java.util.Map;

public class RegisterMolangQueriesEvent {

    @FunctionalInterface
    public interface Callback {
        void onRegisterMolangQueries(RegisterMolangQueriesEvent event);
    }
    private final BiConsumer<String, ToDoubleFunction<MolangInstance>> variable;
    private final BiConsumer<String, Function<List<MathValue>, MathValue>> function;
    private final BiConsumer<String, Function<MolangInstance, Object>> context;
    private final BiConsumer<EntityType<?>, Function<Entity, Map<String, ?>>> actorVariables;
    private final BiConsumer<EntityType<?>, MolangActorVariableWriter> actorVariableWriters;
    private final EntityQueryRegistrar<ToDoubleFunction<MolangInstance>> entityVariable;
    private final EntityQueryRegistrar<Function<List<MathValue>, MathValue>> entityFunction;

    @FunctionalInterface
    public interface EntityQueryRegistrar<T> {
        void register(EntityType<?> type, String name, T provider);
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable) {
        this(variable, (name, factory) -> {
            throw new IllegalStateException("This event does not accept parameterized queries");
        });
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable,
                                     BiConsumer<String, Function<List<MathValue>, MathValue>> function) {
        this(variable, function, (name, provider) -> {
            throw new IllegalStateException("This event does not accept context providers");
        });
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable,
                                     BiConsumer<String, Function<List<MathValue>, MathValue>> function,
                                     BiConsumer<String, Function<MolangInstance, Object>> context) {
        this(variable, function, context, (type, provider) -> {
            throw new IllegalStateException("This event does not accept actor variable providers");
        });
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable,
                                     BiConsumer<String, Function<List<MathValue>, MathValue>> function,
                                     BiConsumer<String, Function<MolangInstance, Object>> context,
                                     BiConsumer<EntityType<?>, Function<Entity, Map<String, ?>>> actorVariables) {
        this(variable, function, context, actorVariables, (type, writer) -> {
            throw new IllegalStateException("This event does not accept actor variable writers");
        });
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable,
                                     BiConsumer<String, Function<List<MathValue>, MathValue>> function,
                                     BiConsumer<String, Function<MolangInstance, Object>> context,
                                     BiConsumer<EntityType<?>, Function<Entity, Map<String, ?>>> actorVariables,
                                     BiConsumer<EntityType<?>, MolangActorVariableWriter> actorVariableWriters) {
        this(variable, function, context, actorVariables, actorVariableWriters,
                (type, name, provider) -> {
                    throw new IllegalStateException("This event does not accept entity-specific variables");
                }, (type, name, provider) -> {
                    throw new IllegalStateException("This event does not accept entity-specific functions");
                });
    }

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable,
                                     BiConsumer<String, Function<List<MathValue>, MathValue>> function,
                                     BiConsumer<String, Function<MolangInstance, Object>> context,
                                     BiConsumer<EntityType<?>, Function<Entity, Map<String, ?>>> actorVariables,
                                     BiConsumer<EntityType<?>, MolangActorVariableWriter> actorVariableWriters,
                                     EntityQueryRegistrar<ToDoubleFunction<MolangInstance>> entityVariable,
                                     EntityQueryRegistrar<Function<List<MathValue>, MathValue>> entityFunction) {
        this.variable = variable;
        this.function = function;
        this.context = context;
        this.actorVariables = actorVariables;
        this.actorVariableWriters = actorVariableWriters;
        this.entityVariable = entityVariable;
        this.entityFunction = entityFunction;
    }

    public void registerVariable(String name, ToDoubleFunction<MolangInstance> value) {
        variable.accept(name, value);
    }

    public void registerFunction(String name, Function<List<MathValue>, MathValue> factory) {
        function.accept(name, factory);
    }

    public void registerEntityVariable(EntityType<?> type, String name, ToDoubleFunction<MolangInstance> value) {
        entityVariable.register(type, name, value);
    }

    public void registerEntityFunction(EntityType<?> type, String name, Function<List<MathValue>, MathValue> factory) {
        entityFunction.register(type, name, factory);
    }

    public void registerContext(String name, Function<MolangInstance, Object> provider) {
        context.accept(name, provider);
    }

    public void registerActorVariables(EntityType<?> type, Function<Entity, Map<String, ?>> provider) {
        actorVariables.accept(type, provider);
    }

    public void registerActorVariableWriter(EntityType<?> type, MolangActorVariableWriter writer) {
        actorVariableWriters.accept(type, writer);
    }
}
