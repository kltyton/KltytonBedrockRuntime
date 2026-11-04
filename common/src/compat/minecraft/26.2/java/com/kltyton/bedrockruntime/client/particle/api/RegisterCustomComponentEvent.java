package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public class RegisterCustomComponentEvent {
    public RegisterCustomComponentEvent() {
    }

    public void register(Identifier id, Codec<? extends IComponent> codec) {
        IComponent.register(id, codec);
    }

    public void register(String vanillaPath, Codec<? extends IComponent> codec) {
        IComponent.register(vanillaPath, codec);
    }
}
