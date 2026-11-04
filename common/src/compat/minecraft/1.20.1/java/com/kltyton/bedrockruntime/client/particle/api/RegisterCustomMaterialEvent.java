package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class RegisterCustomMaterialEvent {

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomMaterialEvent event);
    }

    private final BiFunction<String, Supplier<TextureAtlas>, DescriptionMaterial> factory;

    public RegisterCustomMaterialEvent(BiFunction<String, Supplier<TextureAtlas>, DescriptionMaterial> factory) {
        this.factory = factory;
    }

    public DescriptionMaterial register(String name) {
        return factory.apply(name, null);
    }

    public DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas) {
        return factory.apply(name, atlas);
    }

    public DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas,
                                        ParticleRenderType renderType) {
        return factory.apply(name, atlas).withRenderGroup(renderType);
    }
}
