package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.Tuple;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;

import java.util.*;

public final class EventRandomize implements IEventNode {
    public static final Codec<IEventNode> WEIGHT_CODEC = Codec.FLOAT.orElse(Float.NaN)
            .xmap(EventRandomize::weightNode, EventRandomize::weightValue);

    public static final Codec<EventRandomize> CODEC = IEventNode.CODEC.listOf()
            .xmap(EventRandomize::new, eventRandomize -> eventRandomize.nodes);
    public final List<Map<String, IEventNode>> nodes;

    public final List<Tuple<Float, Map<String, IEventNode>>> sortedNodes;
    private final float totalWeight;

    public EventRandomize(List<Map<String, IEventNode>> nodes) {
        this.sortedNodes = new ArrayList<>();
        List<Map<String, IEventNode>> accepted = new ArrayList<>();
        float total = 0.0F;
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, IEventNode> node = new LinkedHashMap<>(nodes.get(i));
            IEventNode weightNode = node.remove("weight");
            if (!(weightNode instanceof Weight) && !(weightNode instanceof DecimalWeight)) {
                BedrockParticles.LOGGER.warn("Skipping randomize[{}]: weight must be a non-negative number", i);
                continue;
            }
            float weight = weightValue(weightNode);
            if (!(weight >= 0.0F)) {
                BedrockParticles.LOGGER.warn("Skipping randomize[{}]: weight must be a non-negative number", i);
                continue;
            }
            sortedNodes.add(new Tuple<>(weight, node));
            accepted.add(nodes.get(i));
            total += weight;
        }
        this.nodes = List.copyOf(accepted);
        this.totalWeight = total;
    }

    @Override
    public void execute(MolangInstance instance) {
        EventResolver.execute(select(instance), instance);
    }

    public Map<String, IEventNode> select(MolangInstance instance) {
        if (sortedNodes.isEmpty()) return Map.of();
        float choice = instance.getLevel().getRandom().nextFloat() * totalWeight;
        for (Tuple<Float, Map<String, IEventNode>> tuple : sortedNodes) {
            if (choice <= tuple.getA()) return tuple.getB();
            choice -= tuple.getA();
        }
        return sortedNodes.get(sortedNodes.size() - 1).getB();
    }

    @Override
    public String toString() {
        return "EventRandomize[" + "nodes=" + nodes + ']';
    }

    public record Weight(int value) implements IEventNode {
        public static final Codec<Weight> CODEC = Codec.INT.xmap(Weight::new, Weight::value);

        @Override
        public void execute(MolangInstance instance) {}
    }

    public record DecimalWeight(float value) implements IEventNode {
        @Override
        public void execute(MolangInstance instance) {}
    }

    public static IEventNode weightNode(float weight) {
        return Float.isFinite(weight) && weight >= -2147483648.0 && weight < 2147483648.0 && weight == Math.floor(weight)
                ? new Weight((int) weight) : new DecimalWeight(weight);
    }

    public static float weightValue(IEventNode node) {
        float value;
        if (node instanceof Weight weight) value = weight.value();
        else if (node instanceof DecimalWeight weight) value = weight.value();
        else throw new IllegalArgumentException("Random event requires a weight");
        return value;
    }
}
