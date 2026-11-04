package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class RegisterCustomMaterialEvent {
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

    /** Associates a material with a group registered through the loader's particle group registry. */
    public DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas, ParticleRenderType group) {
        return register(name, atlas).withRenderGroup(group);
    }
}
