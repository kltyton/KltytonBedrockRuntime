package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.client.particle.definition.Tuple;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.definition.event.EventResolver;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// Allows for lifetime events on the emitter to trigger certain events.
///
/// All events use the event names in the event section
///
/// All events can be an array or a string
public final class EmitterLifetimeEvents implements IEmitterComponent {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.withDefaultNamespace("emitter_lifetime_events");
    public static final Codec<EmitterLifetimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BedrockParticles.STRING_LIST_CODEC.lenientOptionalFieldOf("creation_event", List.of()).forGetter(events -> events.creationEvent),
            BedrockParticles.STRING_LIST_CODEC.lenientOptionalFieldOf("expiration_event", List.of()).forGetter(events -> events.expirationEvent),
            Codec.unboundedMap(Codec.STRING, BedrockParticles.STRING_LIST_CODEC).lenientOptionalFieldOf("timeline", Map.of()).forGetter(events -> events.timeline),
            Codec.unboundedMap(Codec.STRING, BedrockParticles.STRING_LIST_CODEC).lenientOptionalFieldOf("travel_distance_events", Map.of()).forGetter(events -> events.travelDistanceEvents),
            Codec.list(LoopingTravelDistanceEvent.CODEC).lenientOptionalFieldOf("looping_travel_distance_events", List.of()).forGetter(events -> events.loopingTravelDistanceEvents)
    ).apply(instance, EmitterLifetimeEvents::new));
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;
    public final Map<String, List<String>> travelDistanceEvents;
    public final List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents;

    public final List<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;
    public final List<Tuple<Function<Float, Boolean>, List<String>>> sortedTravelDistance;

    /// @param creationEvent               Fires when the emitter is created
    /// @param expirationEvent             Fires when the emitter expires (does not wait for particles to expire too)
    /// @param timeline                    A series of times, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                    These get fired on every loop the emitter goes through
    /// @param travelDistanceEvents        S series of distances, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                    These get fired when the emitter has moved by the specified input
    /// @param loopingTravelDistanceEvents A series of events that occur at set intervals.<p>
    ///                                    These get fired every time the emitter has moved the specified input distance from the last time it was fired.
    public EmitterLifetimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline, Map<String, List<String>> travelDistanceEvents, List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timeline = timeline;
        this.travelDistanceEvents = travelDistanceEvents;
        this.loopingTravelDistanceEvents = loopingTravelDistanceEvents;

        this.sortedTimeline = new ArrayList<>();
        timeline.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTimeline.add(new Tuple<>(new EventTimelineCondition(tuple.getA()), tuple.getB())));
        this.sortedTravelDistance = new ArrayList<>();
        travelDistanceEvents.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTravelDistance.add(new Tuple<>(new TravelDistanceCondition(tuple.getA()), tuple.getB())));
    }

    @Override
    public Codec<EmitterLifetimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(ParticleEmitter emitter) {
        long previous = emitter.getPreviousElapsedTimeNanos();
        long current = emitter.getElapsedTimeNanos();
        for (int i = emitter.lastTimeline; i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            Function<Integer, Boolean> condition = tuple.getA();
            if (condition instanceof EventTimelineCondition timeline) {
                long timestamp = timeline.timestampNanos();
                if (timestamp >= current) break;
                emitter.lastTimeline = i + 1;
                if (timestamp < previous) continue;
            } else {
                if (!condition.apply(emitter.age)) break;
                emitter.lastTimeline = i + 1;
            }
            executes(emitter, tuple.getB());
        }
        if (emitter.moveDist == emitter.moveDistO) return;
        for (int i = emitter.lastTravelDist; i < sortedTravelDistance.size(); i++) {
            Tuple<Function<Float, Boolean>, List<String>> tuple = sortedTravelDistance.get(i);
            Function<Float, Boolean> condition = tuple.getA();
            if (!condition.apply(emitter.moveDist)) break;
            emitter.lastTravelDist = i + 1;
            if (condition instanceof TravelDistanceCondition travel && travel.distance() < emitter.moveDistO) continue;
            executes(emitter, tuple.getB());
        }
        for (int i = 0; i < loopingTravelDistanceEvents.size(); i++) {
            LoopingTravelDistanceEvent event = loopingTravelDistanceEvents.get(i);
            float previousCycle = (float) Math.floor(emitter.moveDistO / event.distance);
            float currentCycle = (float) Math.floor(emitter.moveDist / event.distance);
            if (Math.abs(currentCycle - previousCycle) >= Math.ulp(1.0F)) {
                emitter.cachedLooping[i] = currentCycle * event.distance;
                executes(emitter, event.effects);
            }
        }
    }

    private record TravelDistanceCondition(float distance) implements Function<Float, Boolean> {
        @Override
        public Boolean apply(Float current) {
            return distance < current;
        }
    }

    @Override
    public void apply(ParticleEmitter emitter) {
        emitter.cachedLooping = new float[loopingTravelDistanceEvents.size()];
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onCreation(ParticleEmitter emitter) {
        executes(emitter, creationEvent);
    }

    public void onExpiration(ParticleEmitter emitter) {
        executes(emitter, expirationEvent);
    }

    @Override
    public String toString() {
        return "EmitterLifetimeEvents[" +
                "creationEvent=" + creationEvent + ", " +
                "expirationEvent=" + expirationEvent + ", " +
                "timeline=" + timeline + ", " +
                "travelDistanceEvents=" + travelDistanceEvents + ", " +
                "loopingTravelDistanceEvents=" + loopingTravelDistanceEvents + ']';
    }

    private static void executes(ParticleEmitter emitter, List<String> triggers) {
        for (String event : triggers) {
            EventResolver.executeEvent(emitter.getPreset().events, event, emitter);
        }
    }

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {
        public static final Codec<LoopingTravelDistanceEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("distance").forGetter(LoopingTravelDistanceEvent::distance),
                BedrockParticles.STRING_LIST_CODEC.fieldOf("effects").forGetter(LoopingTravelDistanceEvent::effects)
        ).apply(instance, LoopingTravelDistanceEvent::new));
    }
}
