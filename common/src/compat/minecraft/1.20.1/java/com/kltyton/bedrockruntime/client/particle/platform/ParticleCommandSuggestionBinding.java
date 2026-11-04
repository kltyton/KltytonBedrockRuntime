package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.particle.command.ParticleCommandSuggestions;
import net.minecraft.commands.SharedSuggestionProvider;

/** Binds live client-side command suggestions for the 1.20.1 runtime. */
public final class ParticleCommandSuggestionBinding {
    private ParticleCommandSuggestionBinding() {
    }

    public static void bind() {
        ParticleCommandSuggestions.bindClient(
                builder -> SharedSuggestionProvider.suggest(
                        MolangParticleEngine.INSTANCE.suggestibleParticleIds().stream()
                                .map(Object::toString)
                                .toList(),
                        builder),
                builder -> SharedSuggestionProvider.suggest(
                        MolangParticleEngine.INSTANCE.suggestibleEmitterIds().stream()
                                .map(String::valueOf)
                                .toList(),
                        builder));
    }
}
