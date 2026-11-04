package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleNetwork;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateSnapshot;
import com.kltyton.bedrockruntime.particle.registry.MolangEntityStateTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/** Adds particle query state to the entity's initial tracking data. */
@Mixin(ServerEntity.class)
public abstract class ServerEntityParticleStateMixin {
    @Shadow @Final private Entity entity;

    @WrapOperation(method = "sendPairingData", at = @At(value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0))
    private void kltyton_bedrock_runtime$addMolangState(Consumer<Object> output, Object spawnPacket,
            Operation<Void> original, @Local(argsOnly = true) ServerPlayer player) {
        if ((entity instanceof Mob || entity instanceof Player) && entity instanceof LivingEntity living
                && !living.isRemoved()
                && ParticleNetwork.canSendToClient(player, MolangEntityStatePacketS2C.TYPE)) {
            MolangEntityStateSnapshot snapshot = MolangEntityStateTracker.snapshot(living);
            if (!living.isRemoved()
                    && ParticleNetwork.canSendToClient(player, MolangEntityStatePacketS2C.TYPE)
                    && MolangEntityStateTracker.isTracking(living, player)) {
                output.accept(new ClientboundCustomPayloadPacket(new MolangEntityStatePacketS2C(
                        snapshot.entityId(), snapshot.entityUuid(), snapshot.flags(),
                        snapshot.itemUseRemaining(), snapshot.recoveryTarget())));
            }
        }
        original.call(output, spawnPacket);
    }
}
