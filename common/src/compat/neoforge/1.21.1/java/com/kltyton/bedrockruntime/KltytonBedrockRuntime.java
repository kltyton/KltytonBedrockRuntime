package com.kltyton.bedrockruntime;

import com.kltyton.bedrockruntime.client.KltytonBedrockRuntimeNeoForgeClient;
import com.kltyton.bedrockruntime.particle.platform.NeoForgeParticlePlatform;
import com.kltyton.bedrockruntime.particle.platform.ParticlePlatforms;
import com.kltyton.bedrockruntime.particle.platform.ParticleServerBootstrap;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;
import com.kltyton.bedrockruntime.network.payload.entity.ObbEntityPartsPayload;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.network.payload.contact.ObbContactReportPayload;
import com.kltyton.bedrockruntime.network.payload.skill.GeoKeyframeSkillPayload;
import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;
import com.kltyton.bedrockruntime.server.network.BoneHitboxServerNetwork;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * CN: NeoForge loader 入口。
 * EN: NeoForge loader entrypoint.
 */
@Mod(Constants.MOD_ID)
public final class KltytonBedrockRuntime {

    /**
     * CN: 创建 NeoForge 入口。
     * EN: Creates the NeoForge entrypoint.
     *
     * @param eventBus the mod event bus
     * @param modContainer the NeoForge mod container
     */
    public KltytonBedrockRuntime(IEventBus eventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, BoneHitboxConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, BoneHitboxConfig.SERVER_SPEC);
        ParticlePlatforms.installCommon(new NeoForgeParticlePlatform(eventBus));
        ParticleServerBootstrap.initializeCommon();
        eventBus.addListener(KltytonBedrockRuntime::registerPayloads);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntime::interactEntity);
        NeoForge.EVENT_BUS.addListener(KltytonBedrockRuntime::serverTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent event) ->
                com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.clear());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClient(eventBus);
        }
        eventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(com.kltyton.bedrockruntime.api.block.shape.VanillaBlockShapes::initializeModBlocks));
        KltytonBedrockRuntimeCommon.init();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("4")
                .playToServer(ObbPartSelectionPayload.TYPE, ObbPartSelectionPayload.CODEC,
                        (payload, context) -> BoneHitboxServerNetwork.handlePartSelection(payload, (ServerPlayer) context.player()))
                .playToServer(ObbEntityPartsPayload.TYPE, ObbEntityPartsPayload.CODEC,
                        (payload, context) -> BoneHitboxServerNetwork.handleEntityParts(payload, (ServerPlayer) context.player()))
                .playToServer(ObbContactReportPayload.TYPE, ObbContactReportPayload.CODEC,
                        (payload, context) -> BoneHitboxServerNetwork.handleContactReport(payload, (ServerPlayer) context.player()))
                .playToServer(GeoKeyframeSkillPayload.TYPE, GeoKeyframeSkillPayload.CODEC,
                        (payload, context) -> BoneHitboxServerNetwork.handleGeoKeyframeSkill(payload, (ServerPlayer) context.player()));
    }

    private static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        InteractionResult result = BoneHitboxServerHooks.handlePlayerInteract(event.getEntity(), event.getHand(), event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.tick(event.getServer());
    }

    private static void registerClient(IEventBus eventBus) {
        try {
            Class<?> clientClass = Class.forName("com.kltyton.bedrockruntime.client.KltytonBedrockRuntimeNeoForgeClient");
            clientClass.getMethod("register", IEventBus.class).invoke(null, eventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register KltytonBedrockRuntime NeoForge client hooks", exception);
        }
    }
}
