package com.kltyton.bedrockruntime.client;

import com.kltyton.bedrockruntime.client.particle.platform.FabricParticleClientPlatform;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientBootstrap;
import com.kltyton.bedrockruntime.client.particle.platform.ParticleClientPlatforms;
import com.kltyton.bedrockruntime.client.compat.geckolib.KltytonBedrockRuntimeFabricGeckoClient;

import com.kltyton.bedrockruntime.client.config.BoneHitboxClientOptions;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.kltyton.bedrockruntime.network.protocol.BoneHitboxNetworking;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

/**
 * CN: Fabric 客户端入口。
 * EN: Fabric client bootstrap.
 */
public final class KltytonBedrockRuntimeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ParticleClientPlatforms.install(new FabricParticleClientPlatform());
        ParticleClientBootstrap.initializeClient();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(
                client -> com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes.initializeModBlocks());
        BoneHitboxNetworking.setClientSelectionSender(payload -> sendIfSupported(ObbPartSelectionPayload.TYPE, payload::write));
        BoneHitboxNetworking.setClientEntityPartsSender(payload -> sendIfSupported(ObbEntityPartsPayload.TYPE, payload::write));
        BoneHitboxNetworking.setClientContactReportSender(payload -> sendIfSupported(ObbContactReportPayload.TYPE, payload::write));
        BoneHitboxNetworking.setClientGeoKeyframeSkillSender(payload -> sendIfSupported(GeoKeyframeSkillPayload.TYPE, payload::write));

        KeyBindingHelper.registerKeyBinding(BoneHitboxClientOptions.HOLD_OBB_KEY);
        registerGeckoRenderers();
        ClientTickEvents.END_CLIENT_TICK.register(BonePartSelectionClient::clientTick);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                BonePartSelectionClient.recordAttack(entity);
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                BonePartSelectionClient.syncCurrentSelection();
            }
            return InteractionResult.PASS;
        });

    }

    private static void sendIfSupported(net.minecraft.resources.ResourceLocation channel,
            java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> encoder) {
        if (ClientPlayNetworking.canSend(channel)) {
            var buffer = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            encoder.accept(buffer);
            ClientPlayNetworking.send(channel, buffer);
        }
    }

    private static void registerGeckoRenderers() {
        KltytonBedrockRuntimeFabricGeckoClient.registerRenderers();
    }
}
