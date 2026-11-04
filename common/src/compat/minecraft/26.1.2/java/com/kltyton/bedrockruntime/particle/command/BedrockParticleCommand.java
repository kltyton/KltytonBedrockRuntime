package com.kltyton.bedrockruntime.particle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.particle.network.EmitterAttachPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterCreationPacketS2C;
import com.kltyton.bedrockruntime.particle.network.EmitterRemovalPacket;

import java.util.Collection;
import java.util.List;

public class BedrockParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.kltyton_bedrock_runtime.particle.failed"));
    private static final List<String> POSITION_SUGGESTIONS = List.of("~ ~ ~", "~ ~1 ~", "~ ~-1 ~", "^ ^ ^", "^ ^ ^1");
    private static final List<String> EXPRESSION_SUGGESTIONS = List.of("\"\"", "\"v.size=1;\"", "\"v.alpha=1;\"", "\"v.size=1;v.alpha=1;\"");
    private static final List<String> ENTITY_SUGGESTIONS = List.of("@s", "@p", "@e[limit=1,sort=nearest]");
    private static final List<String> VIEWER_SUGGESTIONS = List.of("@a", "@p", "@s");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(BedrockParticles.MODID)
                .then(Commands.literal("add").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("particle", IdentifierArgument.id()).suggests(ParticleCommandSuggestions.particleIds()).executes(context -> sendParticle(
                                context.getSource(),
                                IdentifierArgument.getId(context, "particle"),
                                context.getSource().getPosition(),
                                MolangExp.EMPTY,
                                null,
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("pos", Vec3Argument.vec3()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(POSITION_SUGGESTIONS, builder)
                        ).executes(context -> sendParticle(
                                        context.getSource(),
                                        IdentifierArgument.getId(context, "particle"),
                                        Vec3Argument.getVec3(context, "pos"),
                                        MolangExp.EMPTY,
                                        null,
                                        context.getSource().getServer().getPlayerList().getPlayers()
                                )).then(Commands.argument("expression", StringArgumentType.string()).suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(EXPRESSION_SUGGESTIONS, builder)
                                ).executes(context -> sendParticle(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "particle"),
                                                Vec3Argument.getVec3(context, "pos"),
                                                new MolangExp(StringArgumentType.getString(context, "expression")),
                                                null,
                                                context.getSource().getServer().getPlayerList().getPlayers()
                                        )).then(Commands.argument("attach", EntityArgument.entity()).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(ENTITY_SUGGESTIONS, builder)
                                        ).executes(context -> sendParticle(
                                                        context.getSource(),
                                                        IdentifierArgument.getId(context, "particle"),
                                                        Vec3Argument.getVec3(context, "pos"),
                                                        new MolangExp(StringArgumentType.getString(context, "expression")),
                                                        EntityArgument.getEntity(context, "attach"),
                                                        context.getSource().getServer().getPlayerList().getPlayers()
                                                )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                                                ).executes(context -> sendParticle(
                                                                context.getSource(),
                                                                IdentifierArgument.getId(context, "particle"),
                                                                Vec3Argument.getVec3(context, "pos"),
                                                                new MolangExp(StringArgumentType.getString(context, "expression")),
                                                                EntityArgument.getEntity(context, "attach"),
                                                                EntityArgument.getPlayers(context, "viewers")
                                                        ))
                                                )
                                        )
                                )
                        )
                ))
                .then(Commands.literal("remove").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("id", IntegerArgumentType.integer(0)).suggests(ParticleCommandSuggestions.emitterIds()).executes(context -> removeParticle(
                                IntegerArgumentType.getInteger(context, "id"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                        ).executes(context -> removeParticle(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                ))
                .then(Commands.literal("attach").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("id", IntegerArgumentType.integer(0)).suggests(ParticleCommandSuggestions.emitterIds()).then(Commands.argument("entity", EntityArgument.entity()).suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(ENTITY_SUGGESTIONS, builder)
                ).executes(context -> attachEmitter2Entity(
                                IntegerArgumentType.getInteger(context, "id"),
                                EntityArgument.getEntity(context, "entity"),
                                context.getSource().getServer().getPlayerList().getPlayers()
                        )).then(Commands.argument("viewers", EntityArgument.players()).suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(VIEWER_SUGGESTIONS, builder)
                        ).executes(context -> attachEmitter2Entity(
                                        IntegerArgumentType.getInteger(context, "id"),
                                        EntityArgument.getEntity(context, "entity"),
                                        EntityArgument.getPlayers(context, "viewers")
                                )
                        ))
                )))
        );
    }

    private static int attachEmitter2Entity(int id, Entity entity, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            EmitterAttachPacketS2C.sendToClient(serverplayer, id, entity);
            i++;
        }
        BedrockParticles.LOGGER.debug("command attach runtimeId={} entity={} viewers={}", id, entity.getScoreboardName(), viewers.stream().map(ServerPlayer::getScoreboardName).toList());
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            return i;
        }
    }

    private static int removeParticle(int id, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer serverplayer : viewers) {
            EmitterRemovalPacket.sendToClient(serverplayer, id);
            i++;
        }
        BedrockParticles.LOGGER.debug("command remove runtimeId={} viewers={}", id, viewers.stream().map(ServerPlayer::getScoreboardName).toList());
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            return i;
        }
    }

    private static int sendParticle(CommandSourceStack source, Identifier particle, Vec3 pos, MolangExp expression, @Nullable Entity entity, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;
        BedrockParticles.LOGGER.debug("command add particle={} pos={} expression={} attached={} viewers={}",
                particle,
                pos,
                expression == null ? "" : expression.getExpStr(),
                entity == null ? "none" : entity.getScoreboardName(),
                viewers.stream().map(ServerPlayer::getScoreboardName).toList()
        );
        for (ServerPlayer player : viewers) {
            EmitterCreationPacketS2C.sendToClient(player, particle, pos.toVector3f(), expression, entity);
            i++;
        }
        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(() -> Component.translatable("commands.kltyton_bedrock_runtime.particle.add", particle.toString()), true);
            return i;
        }
    }
}
