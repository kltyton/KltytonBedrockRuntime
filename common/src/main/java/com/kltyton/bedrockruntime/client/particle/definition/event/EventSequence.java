package com.kltyton.bedrockruntime.client.particle.definition.event;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;

import java.util.List;
import java.util.Map;

public record EventSequence(List<Map<String, IEventNode>> nodes) implements IEventNode {
    public static final Codec<EventSequence> CODEC = IEventNode.CODEC.listOf().xmap(EventSequence::new, EventSequence::nodes);

    @Override
    public void execute(MolangInstance instance) {
        for (Map<String, IEventNode> nodeMap : nodes) {
            EventResolver.execute(nodeMap, instance);
        }
    }
}
