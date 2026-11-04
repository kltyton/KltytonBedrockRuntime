package com.kltyton.bedrockruntime.particle.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Named providers synchronized with the command tree and populated on the client. */
public final class ParticleCommandSuggestions {
    private static SuggestionProvider<CommandSourceStack> particleIds;
    private static SuggestionProvider<CommandSourceStack> emitterIds;
    private static volatile Function<SuggestionsBuilder, CompletableFuture<Suggestions>> clientParticles;
    private static volatile Function<SuggestionsBuilder, CompletableFuture<Suggestions>> clientEmitters;

    private ParticleCommandSuggestions() {
    }

    public static void register() {
        particleIds = SuggestionProviders.register(BedrockParticles.asResource("particle_ids"),
                (context, builder) -> suggest(context, builder, false));
        emitterIds = SuggestionProviders.register(BedrockParticles.asResource("emitter_ids"),
                (context, builder) -> suggest(context, builder, true));
    }

    public static void bindClient(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> particles,
                                  Function<SuggestionsBuilder, CompletableFuture<Suggestions>> emitters) {
        clientParticles = Objects.requireNonNull(particles);
        clientEmitters = Objects.requireNonNull(emitters);
    }

    public static SuggestionProvider<CommandSourceStack> particleIds() {
        return Objects.requireNonNull(particleIds, "Particle suggestions not registered");
    }

    public static SuggestionProvider<CommandSourceStack> emitterIds() {
        return Objects.requireNonNull(emitterIds, "Emitter suggestions not registered");
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<SharedSuggestionProvider> context,
                                                           SuggestionsBuilder builder, boolean emitter) {
        if (context.getSource() instanceof CommandSourceStack) {
            if (emitter && builder.getRemaining().isEmpty()) builder.suggest(0);
            return builder.buildFuture();
        }
        Function<SuggestionsBuilder, CompletableFuture<Suggestions>> provider = emitter ? clientEmitters : clientParticles;
        return Objects.requireNonNull(provider, "Client particle suggestions not initialized").apply(builder);
    }
}
