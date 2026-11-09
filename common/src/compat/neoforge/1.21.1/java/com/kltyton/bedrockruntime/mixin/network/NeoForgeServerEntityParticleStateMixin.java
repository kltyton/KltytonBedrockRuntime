package com.kltyton.bedrockruntime.mixin.network;

import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleNetwork;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateSnapshot;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Adds particle query state immediately before the entity spawn packet in the tracking bundle. */
@Mixin(ServerEntity.class)
public abstract class NeoForgeServerEntityParticleStateMixin {
    @Shadow @Final private Entity entity;

    @WrapOperation(method = "sendPairingData", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/network/bundle/PacketAndPayloadAcceptor;accept(Lnet/minecraft/network/protocol/Packet;)Lnet/neoforged/neoforge/network/bundle/PacketAndPayloadAcceptor;", ordinal = 0))
    private PacketAndPayloadAcceptor<ClientGamePacketListener> kltyton_bedrock_runtime$addMolangState(
            PacketAndPayloadAcceptor<ClientGamePacketListener> output, Packet<? super ClientGamePacketListener> spawnPacket,
            Operation<PacketAndPayloadAcceptor<ClientGamePacketListener>> original, @Local(argsOnly = true) ServerPlayer player) {
        if ((entity instanceof Mob || entity instanceof Player) && entity instanceof LivingEntity living
                && !living.isRemoved() && ParticleNetwork.canSendToClient(player, MolangEntityStatePacketS2C.TYPE)) {
            MolangEntityStateSnapshot snapshot = MolangEntityStateTracker.snapshot(living);
            if (!living.isRemoved() && ParticleNetwork.canSendToClient(player, MolangEntityStatePacketS2C.TYPE)
                    && MolangEntityStateTracker.isTracking(living, player)) {
                original.call(output, new ClientboundCustomPayloadPacket(new MolangEntityStatePacketS2C(
                        snapshot.entityId(), snapshot.entityUuid(), snapshot.flags(),
                        snapshot.itemUseRemaining(), snapshot.recoveryTarget())));
            }
        }
        return original.call(output, spawnPacket);
    }
}
