package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.serialization.Codec;

public class RegisterCustomEventNodeEvent {

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomEventNodeEvent event);
    }

    public RegisterCustomEventNodeEvent() {
    }

    public void register(String name, Codec<? extends IEventNode> codec) {
        IEventNode.register(name, codec);
    }
}
