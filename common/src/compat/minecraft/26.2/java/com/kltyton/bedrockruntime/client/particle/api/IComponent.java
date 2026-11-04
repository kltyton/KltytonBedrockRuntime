package com.kltyton.bedrockruntime.client.particle.api;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

public interface IComponent {
    HashBiMap<Identifier, Codec<IComponent>> COMPONENTS = HashBiMap.create();

    @SuppressWarnings("unchecked")
    static void register(Identifier id, Codec<? extends IComponent> codec) {
        COMPONENTS.put(id, (Codec<IComponent>) codec);
    }

    static void register(String vanillaPath, Codec<? extends IComponent> codec) {
        register(Identifier.withDefaultNamespace(vanillaPath), codec);
    }

    Codec<? extends IComponent> codec();

    List<? extends MolangExp> getAllMolangExp();

    /// @return order < 0 means early initialize and no update
    default int order() {
        return 1000;
    }
}
