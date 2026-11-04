package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import com.kltyton.bedrockruntime.client.particle.definition.Tuple;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.event.EventResolver;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// All events use the event names in the event section
///
/// All events can be either an array or a string
public final class ParticleLifeTimeEvents implements IParticleComponent {
    public static final Identifier ID = Identifier.withDefaultNamespace("particle_lifetime_events");
    public static final Codec<ParticleLifeTimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BedrockParticles.STRING_LIST_CODEC.lenientOptionalFieldOf("creation_event", List.of()).forGetter(events -> events.creationEvent),
            BedrockParticles.STRING_LIST_CODEC.lenientOptionalFieldOf("expiration_event", List.of()).forGetter(events -> events.expirationEvent),
            Codec.unboundedMap(Codec.STRING, BedrockParticles.STRING_LIST_CODEC).lenientOptionalFieldOf("timeline", Map.of()).forGetter(events -> events.timeline)
    ).apply(instance, ParticleLifeTimeEvents::new));
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;

    public final List<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;

    /// @param creationEvent   Fires when the particle is created
    /// @param expirationEvent Fires when the particle expires (does not wait for particles to expire too)
    /// @param timeline        A series of times, e.g. 0.0 or 1.0, that trigger the event
    public ParticleLifeTimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timeline = timeline;

        this.sortedTimeline = new ArrayList<>();
        timeline.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTimeline.add(new Tuple<>(new EventTimelineCondition(tuple.getA()), tuple.getB())));
    }

    @Override
    public Codec<ParticleLifeTimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        long current = instance.getElapsedTimeNanos();
        long previous = Math.max(0L, current - com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock.nanos(instance.getInvTickRate()));
        update(instance, previous, current);
    }

    public void update(IMolangParticleInstance instance, long previous, long current) {
        if (sortedTimeline.isEmpty()) return;
        EventResolver.executeBatch(instance, () -> updateTimeline(instance, previous, current));
    }

    private void updateTimeline(IMolangParticleInstance instance, long previous, long current) {
        for (int i = instance.getLastTimeline(); i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            Function<Integer, Boolean> condition = tuple.getA();
            if (condition instanceof EventTimelineCondition timeline) {
                long timestamp = timeline.timestampNanos();
                if (timestamp >= current) break;
                instance.setLastTimeline(i + 1);
                if (timestamp < previous) continue;
            } else {
                if (!condition.apply(instance.getAge())) break;
                instance.setLastTimeline(i + 1);
            }
            executeInBatch(instance, tuple.getB());
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        executes(instance, creationEvent);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onExpiration(IMolangParticleInstance instance) {
        executes(instance, expirationEvent);
    }

    @Override
    public String toString() {
        return "ParticleLifeTimeEvents[" +
                "creationEvent=" + creationEvent + ", " +
                "expirationEvent=" + expirationEvent + ", " +
                "timeline=" + timeline + ']';
    }

    private static void executes(IMolangParticleInstance instance, List<String> triggers) {
        if (triggers.isEmpty()) return;
        EventResolver.executeBatch(instance, () -> executeInBatch(instance, triggers));
    }

    private static void executeInBatch(IMolangParticleInstance instance, List<String> triggers) {
        for (String event : triggers) {
            EventResolver.executeInBatch(instance.getPreset().effect.events, event, instance);
        }
    }
}