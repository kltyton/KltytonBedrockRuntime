package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.serialization.Codec;
import java.util.Hashtable;
import java.util.Map;

public interface IEventNode {
    Codec<Map<String, IEventNode>> CODEC = Codec.dispatchedMap(Codec.STRING, IEventNode::getCodec);
    Map<String, Codec<IEventNode>> MAP = new Hashtable<>();

    void execute(MolangInstance instance);

    @SuppressWarnings("unchecked")
    static <T extends IEventNode> Codec<T> getCodec(String name) {
        Codec<T> codec = (Codec<T>) MAP.get(name);
        if (codec == null) throw new IllegalArgumentException("Unknown particle event type: " + name);
        return codec;
    }

    @SuppressWarnings("unchecked")
    static void register(String name, Codec<? extends IEventNode> codec) {
        MAP.put(name, (Codec<IEventNode>) codec);
    }
}
