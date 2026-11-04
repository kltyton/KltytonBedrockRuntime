package com.kltyton.bedrockruntime.server.collision.contact;

import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.event.BoneHitboxEvents;

import net.minecraft.world.entity.player.Player;

/**
 * CN: 派发已经由服务端规范化去重的客户端 OBB 接触；本类不执行几何计算。
 * EN: Dispatches client OBB contacts already canonicalized and deduplicated by the server; this class performs no geometry tests.
 */
public final class BoneHitboxCollisionHooks {
    private BoneHitboxCollisionHooks() {
    }

    public static void handleClientContact(ObbCollisionContext context) {
        if (context.firstEntity() instanceof BoneHitboxEntity first) {
            first.kltyton_bedrock_runtime$onObbCollision(context);
        }
        if (context.secondEntity() instanceof BoneHitboxEntity second) {
            second.kltyton_bedrock_runtime$onObbCollision(context.reversed());
        }

        BoneHitboxEvents.fireEntityPartCollision(context);
        if (context.firstEntity() instanceof Player) {
            BoneHitboxEvents.firePlayerPartCollision(context);
        }
        if (context.secondEntity() instanceof Player) {
            BoneHitboxEvents.firePlayerPartCollision(context.reversed());
        }
    }
}
