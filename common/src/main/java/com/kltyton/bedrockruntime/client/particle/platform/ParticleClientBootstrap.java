package com.kltyton.bedrockruntime.client.particle.platform;

/** Client-side particle bootstrap called from each loader client entrypoint. */
public final class ParticleClientBootstrap {
    private ParticleClientBootstrap() {
    }

    public static void initializeClient() {
        ParticleClientPlatforms.require().initializeClient();
        ParticleCommandSuggestionBinding.bind();
    }
}
