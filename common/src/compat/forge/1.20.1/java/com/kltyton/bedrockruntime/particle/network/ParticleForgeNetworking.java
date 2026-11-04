package com.kltyton.bedrockruntime.particle.network;

import com.kltyton.bedrockruntime.Constants;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Forge 1.20.1 transport. Message types are registered in both directions; consumers are side-local. */
public final class ParticleForgeNetworking {
    public static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Constants.id("particles"),
            () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));
    private static volatile BiConsumer<Object, Supplier<NetworkEvent.Context>> clientConsumer;

    private ParticleForgeNetworking() {
    }

    public static void setClientConsumer(BiConsumer<Object, Supplier<NetworkEvent.Context>> consumer) {
        clientConsumer = consumer;
    }

    public static void register() {
        CHANNEL.messageBuilder(EmitterCreationPacketS2C.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitterCreationPacketS2C::write).decoder(EmitterCreationPacketS2C::new)
                .consumerMainThread(ParticleForgeNetworking::consumeClient).add();
        CHANNEL.messageBuilder(EmitterAttachPacketS2C.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitterAttachPacketS2C::write).decoder(EmitterAttachPacketS2C::new)
                .consumerMainThread(ParticleForgeNetworking::consumeClient).add();
        CHANNEL.messageBuilder(EmitterRemovalPacketS2C.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitterRemovalPacketS2C::write).decoder(EmitterRemovalPacketS2C::new)
                .consumerMainThread(ParticleForgeNetworking::consumeClient).add();
        CHANNEL.messageBuilder(MolangEntityStatePacketS2C.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MolangEntityStatePacketS2C::write).decoder(MolangEntityStatePacketS2C::new)
                .consumerMainThread(ParticleForgeNetworking::consumeClient).add();
        CHANNEL.messageBuilder(EmitterSynchronizePacketS2C.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitterSynchronizePacketS2C::write).decoder(EmitterSynchronizePacketS2C::new)
                .consumerMainThread(ParticleForgeNetworking::consumeClient).add();
        CHANNEL.messageBuilder(EmitterRemovalPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EmitterRemovalPacket::write).decoder(EmitterRemovalPacket::new)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        ParticleServerPacketHandler.handleRemoval(player, payload);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(EmitterSynchronizePacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EmitterSynchronizePacket::write).decoder(EmitterSynchronizePacket::new)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        ParticleServerPacketHandler.handleSynchronize(player, payload);
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void consumeClient(Object payload, Supplier<NetworkEvent.Context> context) {
        BiConsumer<Object, Supplier<NetworkEvent.Context>> consumer = clientConsumer;
        if (consumer != null) {
            consumer.accept(payload, context);
        } else {
            context.get().setPacketHandled(true);
        }
    }

    public static boolean canSendTo(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static boolean canSendToServer(Connection connection) {
        return connection != null && CHANNEL.isRemotePresent(connection);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        if (canSendTo(player)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static Packet<?> toClientPacket(Object payload) {
        return CHANNEL.toVanillaPacket(payload, NetworkDirection.PLAY_TO_CLIENT);
    }
}
