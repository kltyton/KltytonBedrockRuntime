package com.kltyton.bedrockruntime.client.particle.network;

import com.kltyton.bedrockruntime.particle.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.molang.MolangEntityStateInbox;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/** Client handling for emitter runtime payloads. */
public final class PSClientPacketHandler {
    private PSClientPacketHandler() {}

    public static void handleMolangEntityState(MolangEntityStatePacketS2C packet, Player player) {
        MolangEntityStateInbox.receive((ClientLevel) player.level(), packet);
    }

    public static void handleEmitterAttach(EmitterAttachPacketS2C packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) handleEmitterAttach(packet, player);
    }

    public static void handleEmitterAttach(EmitterAttachPacketS2C packet, Player player) {
        ParticleEmitter emitter = MolangParticleEngine.INSTANCE._getEmitter(packet.particleId());
        if (emitter != null) {
            Entity entity = player.level().getEntity(packet.entityId());
            if (entity != null) emitter.attachEntity(entity);
        }
    }

    public static void handleEmitterCreation(EmitterCreationPacketS2C packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) handleEmitterCreation(packet, player);
    }

    public static void handleEmitterCreation(EmitterCreationPacketS2C packet, Player player) {
        Identifier resolved = MolangParticleEngine.INSTANCE.resolveParticleId(packet.id());
        if (resolved == null) {
            BedrockParticles.LOGGER.warn("Unknown particle id in emitter packet: {}", packet.id());
            return;
        }
        try {
            Entity attached = packet.entityId() > 0 ? player.level().getEntity(packet.entityId()) : null;
            PSDiagnostics.infoFirstN("packet-client:" + resolved, 32,
                    "client received emitter packet requested={} resolved={} pos=({}, {}, {}) expression={} entityId={} attached={}",
                    packet.id(), resolved, packet.pos().x, packet.pos().y, packet.pos().z,
                    packet.expression() == null ? "" : packet.expression().getExpStr(), packet.entityId(),
                    attached == null ? "none" : attached.getScoreboardName());
            ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(packet.pos().x, packet.pos().y, packet.pos().z),
                    resolved, packet.expression(), attached == null ? null : IEntity.of(attached));
            MolangParticleEngine.INSTANCE.addEmitter(emitter, false);
            PSDiagnostics.infoFirstN("packet-client-added:" + resolved, 32,
                    "client added emitter runtimeId={} particle={} pos={} attached={}",
                    emitter.id, emitter.particleId, emitter.pos,
                    attached == null ? "none" : attached.getScoreboardName());
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("Failed to create emitter {} at {}", packet.id(), packet.pos(), exception);
            throw exception;
        }
    }

    public static void handleEmitterRemoval(EmitterRemovalPacketS2C packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) handleEmitterRemoval(packet.id(), player);
    }

    public static void handleEmitterRemoval(int id, Player player) {
        ParticleEmitter emitter = MolangParticleEngine.INSTANCE.removeEmitter(id, false);
        if (emitter == null) {
            player.sendSystemMessage(Component.translatable("commands.kltyton_bedrock_runtime.particle.not_found", id));
        } else {
            player.sendSystemMessage(Component.translatable("commands.kltyton_bedrock_runtime.particle.remove",
                    emitter.particleId == null ? id : emitter.particleId.toString()));
        }
    }

    public static void handleEmitterSynchronize(EmitterSynchronizePacket packet, Player player) {
        MolangParticleEngine.INSTANCE.loadEmitter(player.level(), packet.id(), packet.tag());
    }

    public static void synchronizeEmitterToServer(ParticleEmitter emitter) {
        if (ParticleNetwork.canSendToServer(EmitterSynchronizePacket.TYPE)) {
            ParticleNetwork.sendToServer(new EmitterSynchronizePacket(emitter.id, emitter.serialize()));
        }
    }

    public static void sendEmitterRemovalToServer(int id) {
        if (ParticleNetwork.canSendToServer(EmitterRemovalPacket.TYPE)) {
            ParticleNetwork.sendToServer(new EmitterRemovalPacket(id));
        }
    }

}
