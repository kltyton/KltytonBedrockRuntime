package com.kltyton.bedrockruntime.server.combat.attack;

import com.kltyton.bedrockruntime.api.context.attack.ObbAttackContext;
import com.kltyton.bedrockruntime.api.context.collision.ObbCollisionContext;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.event.BoneHitboxEvents;

import net.minecraft.world.entity.player.Player;

/**
 * CN: 派发纯 OBB 接触产生的攻击/受击事件；本路径不会调用原版 hurt。
 * EN: Dispatches attack/hurt events produced solely by OBB contact; this path never calls vanilla hurt.
 */
public final class BoneHitboxContactAttackHooks {
    private BoneHitboxContactAttackHooks() {
    }

    public static void handleContact(ObbCollisionContext collision) {
        ObbAttackContext context = ObbAttackContext.obbContact(collision);
        if (collision.firstEntity() instanceof BoneHitboxEntity attacker
                && attacker.kltyton_bedrock_runtime$beforeObbCollisionAttack(context)) {
            attacker.kltyton_bedrock_runtime$onObbCollisionAttack(context);
            BoneHitboxEvents.fireEntityObbCollisionAttack(context);
            if (collision.firstEntity() instanceof Player) {
                BoneHitboxEvents.firePlayerObbCollisionAttack(context);
            }
            attacker.kltyton_bedrock_runtime$afterObbCollisionAttack(context);
        }

        if (collision.secondEntity() instanceof BoneHitboxEntity target
                && target.kltyton_bedrock_runtime$beforeObbCollisionHurt(context)) {
            target.kltyton_bedrock_runtime$onObbCollisionHurt(context);
            BoneHitboxEvents.fireEntityObbCollisionHurt(context);
            if (collision.secondEntity() instanceof Player) {
                BoneHitboxEvents.firePlayerObbCollisionHurt(context);
            }
            target.kltyton_bedrock_runtime$afterObbCollisionHurt(context);
        }
    }
}
