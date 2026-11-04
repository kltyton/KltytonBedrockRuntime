package com.kltyton.bedrockruntime.client.particle.api;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public class RegisterCustomComponentEvent {

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomComponentEvent event);
    }

    public RegisterCustomComponentEvent() {
    }

    public void register(ResourceLocation id, Codec<? extends IComponent> codec) {
        IComponent.register(id, codec);
    }

    public void register(String vanillaPath, Codec<? extends IComponent> codec) {
        IComponent.register(vanillaPath, codec);
    }
}
