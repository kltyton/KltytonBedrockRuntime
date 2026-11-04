package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.serialization.Codec;

public class RegisterCustomEventNodeEvent {
    public RegisterCustomEventNodeEvent() {
    }

    public void register(String name, Codec<? extends IEventNode> codec) {
        IEventNode.register(name, codec);
    }
}
