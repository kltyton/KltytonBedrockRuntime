package com.kltyton.bedrockruntime.event;

import com.kltyton.bedrockruntime.server.hook.vanilla.BoneHitboxServerHooks;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore;
import com.kltyton.bedrockruntime.server.skill.keyframe.GeoKeyframeSkillDispatcher;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

/**
 * CN: Fabric 玩法事件桥。
 * EN: Fabric gameplay event bridge.
 */
public final class KltytonBedrockRuntimeFabricEvents {
    private KltytonBedrockRuntimeFabricEvents() {
    }

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.kltyton.bedrockruntime.server.sync.ObbServerLifecycle.tick(server);
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                level.isClientSide() ? InteractionResult.PASS : BoneHitboxServerHooks.handlePlayerInteract(player, hand, entity));
    }
}
