package com.kltyton.bedrockruntime.client.particle.network;

import com.kltyton.bedrockruntime.particle.network.*;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.molang.MolangEntityStateInbox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

/** Client-side handling for emitter and editor packets. */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public final class PSClientPacketHandler {
    private PSClientPacketHandler() {
    }

    public static void handleMolangEntityState(MolangEntityStatePacketS2C packet, Player player) {
        if (player.level() instanceof ClientLevel level) {
            MolangEntityStateInbox.receive(level, packet);
        }
    }

    public static void handleEmitterAttach(EmitterAttachPacketS2C packet, Player player) {
        ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(packet.particleId());
        Entity entity = player.level().getEntity(packet.entityId());
        if (emitter != null && entity != null) {
            emitter.attachEntity(entity);
        }
    }

    public static void handleEmitterCreation(EmitterCreationPacketS2C packet, Player player) {
        var resolved = MolangParticleEngine.INSTANCE.resolveParticleId(packet.id());
        if (resolved == null) {
            PSDiagnostics.warn("ignoring unknown particle id from network requested={} knownIds={}",
                    packet.id(), MolangParticleEngine.INSTANCE.suggestibleParticleIds());
            return;
        }

        try {
            Entity attached = packet.entityId() > 0 ? player.level().getEntity(packet.entityId()) : null;
            PSDiagnostics.infoFirstN("packet-client:" + resolved, 32,
                    "client received emitter packet requested={} resolved={} pos=({}, {}, {}) expression={} entityId={} attached={}",
                    packet.id(), resolved, packet.pos().x, packet.pos().y, packet.pos().z,
                    packet.expression() == null ? "" : packet.expression().getExpStr(), packet.entityId(),
                    attached == null ? "none" : attached.getScoreboardName());
            ParticleEmitter emitter = new ParticleEmitter(player.level(),
                    new Vec3(packet.pos().x, packet.pos().y, packet.pos().z), resolved, packet.expression(),
                    attached == null ? null : IEntity.of(attached));
            MolangParticleEngine.INSTANCE.addEmitter(emitter, false);
            PSDiagnostics.infoFirstN("packet-client-added:" + resolved, 32,
                    "client added emitter runtimeId={} particle={} pos={} attached={}",
                    emitter.id, emitter.particleId, emitter.pos,
                    attached == null ? "none" : attached.getScoreboardName());
        } catch (RuntimeException exception) {
            BedrockParticles.LOGGER.error("Failed to create emitter requested={} resolved={} pos={} expression={}",
                    packet.id(), resolved, packet.pos(), packet.expression(), exception);
            throw exception;
        }
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
        if (ClientPlayNetworking.canSend(EmitterSynchronizePacket.TYPE)) {
            ClientPlayNetworking.send(new EmitterSynchronizePacket(emitter.id, emitter.serialize()));
        }
    }

    public static void sendEmitterRemovalToServer(int id) {
        if (ClientPlayNetworking.canSend(EmitterRemovalPacket.TYPE)) {
            ClientPlayNetworking.send(new EmitterRemovalPacket(id));
        }
    }

}
