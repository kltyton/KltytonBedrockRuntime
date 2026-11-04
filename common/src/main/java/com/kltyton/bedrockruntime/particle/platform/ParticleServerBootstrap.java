package com.kltyton.bedrockruntime.particle.platform;

import com.kltyton.bedrockruntime.particle.command.ParticleCommandSuggestions;

/** Common/server particle bootstrap called from loader common entrypoints. */
public final class ParticleServerBootstrap {
    private static boolean commandSuggestionsRegistered;

    private ParticleServerBootstrap() {
    }

    public static void initializeCommon() {
        if (!commandSuggestionsRegistered) {
            commandSuggestionsRegistered = true;
            ParticleCommandSuggestions.register();
        }
        ParticlePlatforms.requireCommon().initializeCommon();
    }
}
