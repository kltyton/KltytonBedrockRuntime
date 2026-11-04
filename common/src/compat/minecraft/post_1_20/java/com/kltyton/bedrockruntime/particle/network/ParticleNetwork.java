package com.kltyton.bedrockruntime.particle.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Loader-neutral transport facade for {@link CustomPacketPayload} targets (1.21.1+).
 * Payload classes stay pure data; loader adapters install the actual transport.
 */
public final class ParticleNetwork {
    public interface ClientTransport {
        boolean canSend(CustomPacketPayload.Type<?> type);

        void sendToServer(CustomPacketPayload payload);
    }

    public interface ServerTransport {
        boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

        void sendToTracking(ServerLevel level, Entity entity, CustomPacketPayload payload,
                            Predicate<ServerPlayer> filter);
    }

    private static volatile ClientTransport client;
    private static volatile ServerTransport server;

    private ParticleNetwork() {
    }

    public static void installClient(ClientTransport transport) {
        if (client != null && client != transport) {
            throw new IllegalStateException("Particle client transport already installed");
        }
        client = transport;
    }

    public static void installServer(ServerTransport transport) {
        if (server != null && server != transport) {
            throw new IllegalStateException("Particle server transport already installed");
        }
        server = transport;
    }

    public static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        ClientTransport transport = client;
        return transport != null && transport.canSend(type);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientTransport transport = client;
        if (transport != null && transport.canSend(payload.type())) {
            transport.sendToServer(payload);
        }
    }

    public static boolean canSendToClient(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        ServerTransport transport = server;
        return transport != null && transport.canSend(player, type);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerTransport transport = server;
        if (transport != null && transport.canSend(player, payload.type())) {
            transport.sendToPlayer(player, payload);
        }
    }

    public static void sendToTracking(ServerLevel level, Entity entity, CustomPacketPayload payload,
                                      Predicate<ServerPlayer> filter) {
        ServerTransport transport = server;
        if (transport != null) {
            transport.sendToTracking(level, entity, payload, filter);
        }
    }

    public static boolean isServerInstalled() {
        return server != null;
    }

    public static boolean isClientInstalled() {
        return client != null;
    }
}
