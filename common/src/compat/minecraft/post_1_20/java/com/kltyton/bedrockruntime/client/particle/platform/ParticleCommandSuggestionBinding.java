package com.kltyton.bedrockruntime.client.particle.platform;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleManager;
import com.kltyton.bedrockruntime.particle.command.ParticleCommandSuggestions;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.HashSet;
import java.util.Set;

/** Binds live client-side command suggestions for the post-1.20 runtime. */
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
                builder -> {
                    MolangParticleManager manager = MolangParticleEngine.getScreenManager();
                    if (manager == null) manager = MolangParticleEngine.INSTANCE;
                    Set<Integer> ids = new HashSet<>();
                    var iterator = manager.getEmitters();
                    while (iterator.hasNext()) {
                        ids.add(iterator.next().getIntKey());
                    }
                    return SharedSuggestionProvider.suggest(
                            ids.stream().map(String::valueOf).toList(),
                            builder);
                });
    }
}
