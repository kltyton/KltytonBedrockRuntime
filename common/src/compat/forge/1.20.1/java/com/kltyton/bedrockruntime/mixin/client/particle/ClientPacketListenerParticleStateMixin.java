package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.molang.MolangEntityStateInbox;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import com.kltyton.bedrockruntime.particle.network.ParticleForgeNetworking;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes initial query state to the incoming entity packet bundle. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerParticleStateMixin {
    @Shadow private ClientLevel level;

    @WrapMethod(method = "handleBundlePacket")
    private void kltyton_bedrock_runtime$withEntityStateBundle(ClientboundBundlePacket packet, Operation<Void> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread() || minecraft.getConnection() == null
                || !ParticleForgeNetworking.canSendToServer(minecraft.getConnection().getConnection())) {
            original.call(packet);
            return;
        }
        try (MolangEntityStateInbox.Scope ignored = MolangEntityStateInbox.enterBundle()) {
            original.call(packet);
        }
    }

    @Inject(method = "handleAddEntity", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$finishEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo callback) {
        MolangEntityStateInbox.forgetInitial(level, packet.getId(), packet.getUUID());
    }
}
