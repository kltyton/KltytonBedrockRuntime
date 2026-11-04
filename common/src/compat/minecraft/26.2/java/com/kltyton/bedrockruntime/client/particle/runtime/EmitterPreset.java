package com.kltyton.bedrockruntime.client.particle.runtime;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import net.minecraft.core.particles.ParticleType;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.EmitterPresetLoadedEvent;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.definition.component.*;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmitterPreset {
    public final ParticleType<?> type;
    public final List<IEmitterComponent> components;
    public final List<EmitterShape> shapes;
    public final boolean hasInstantRate;
    public final boolean hasSteadyRate;
    public final Map<String, Map<String, IEventNode>> events;
    public final VariableTable vars;
    public final MolangVersion molangVersion;
    public final boolean bedrockItemNames;
    public EmitterRate.Type emitterRateType = EmitterRate.Type.MANUAL;
    public boolean localPosition = false;
    public boolean localRotation = false;
    public boolean localVelocity = false;
    public EmitterLifetimeEvents lifetimeEvents;

    /// For custom preset data
    protected Map<Class<?>, Object> tickets;

    public EmitterPreset(ParticleType<?> type, List<IEmitterComponent> components, Map<String, Map<String, IEventNode>> events) {
        this(type, components, events, MolangVersion.LATEST);
    }

    public EmitterPreset(ParticleType<?> type, List<IEmitterComponent> components,
                         Map<String, Map<String, IEventNode>> events, MolangVersion molangVersion) {
        this(type, components, events, molangVersion, false);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public EmitterPreset(ParticleType<?> type, List<IEmitterComponent> components,
                         Map<String, Map<String, IEventNode>> events, MolangVersion molangVersion,
                         boolean bedrockItemNames) {
        this.type = type;
        boolean hasShape = components.stream().anyMatch(component -> component instanceof EmitterShape
                || component instanceof EmitterMultiShape);
        if (hasShape) {
            this.components = components;
        } else {
            this.components = new ArrayList<>(components);
            this.components.add(new EmitterShape.Custom(FloatMolangExp3.ZERO, FloatMolangExp3.ZERO));
        }
        this.shapes = this.components.stream().filter(EmitterShape.class::isInstance)
                .map(EmitterShape.class::cast).toList();
        this.hasInstantRate = this.components.stream().anyMatch(EmitterRate.Instant.class::isInstance);
        this.hasSteadyRate = this.components.stream().anyMatch(EmitterRate.Steady.class::isInstance);
        this.events = events;
        this.molangVersion = molangVersion;
        this.bedrockItemNames = bedrockItemNames;
        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table, molangVersion, bedrockItemNames);
        for (IEmitterComponent component : this.components) {
            if (component instanceof EmitterRate) {
                switch (component) {
                    case EmitterRate.Instant ignored -> this.emitterRateType = EmitterRate.Type.INSTANT;
                    case EmitterRate.Steady ignored -> this.emitterRateType = EmitterRate.Type.STEADY;
                    default -> this.emitterRateType = EmitterRate.Type.MANUAL;
                }
            } else if (component instanceof EmitterLocalSpace(boolean position, boolean rotation, boolean velocity)) {
                this.localPosition = position;
                this.localRotation = rotation;
                this.localVelocity = velocity;
            } else if (component instanceof EmitterLifetimeEvents e) {
                this.lifetimeEvents = e;
            }
            for (MolangExp exp : component.getAllMolangExp()) {
                exp.compile(parser);
            }
            if (component instanceof EmitterMultiShape multi) {
                for (var shapeEntry : multi.shapes()) {
                    for (MolangExp exp : shapeEntry.value().getAllMolangExp()) exp.compile(parser);
                }
            }
        }

        this.vars = table;
        ParticleEvents.post(new EmitterPresetLoadedEvent(this));
    }

    public <T> void setTicket(Class<T> clazz, T value) {
        if (tickets == null) this.tickets = new HashMap<>();
        tickets.put(clazz, value);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getTicket(Class<T> clazz) {
        return tickets == null ? null : (T) tickets.get(clazz);
    }

    private static Hashtable<String, Variable> addDefaultVariables() {
        Hashtable<String, Variable> table = new Hashtable<>();
        table.put("variable.emitter_age", new Variable("variable.emitter_age", 0.0));
        table.put("variable.emitter_lifetime", new Variable("variable.emitter_lifetime", 0.0));
        table.put("variable.emitter_random_1", new Variable("variable.emitter_random_1", 0.0));
        table.put("variable.emitter_random_2", new Variable("variable.emitter_random_2", 0.0));
        table.put("variable.emitter_random_3", new Variable("variable.emitter_random_3", 0.0));
        table.put("variable.emitter_random_4", new Variable("variable.emitter_random_4", 0.0));
        table.put("variable.entity_scale", new Variable("variable.entity_scale", 0.0));
        return table;
    }
}
