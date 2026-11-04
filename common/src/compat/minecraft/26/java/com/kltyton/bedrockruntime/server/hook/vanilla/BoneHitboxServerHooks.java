package com.kltyton.bedrockruntime.server.hook.vanilla;

import com.kltyton.bedrockruntime.server.combat.attack.BoneHitboxAttackBoxHooks;
import com.kltyton.bedrockruntime.server.combat.damage.VanillaDamageCapture;
import com.kltyton.bedrockruntime.server.selection.ServerPartSelectionStore;

import java.util.Optional;

import com.kltyton.bedrockruntime.api.bone.attribute.ObbBoneAttribute;
import com.kltyton.bedrockruntime.api.bone.builtin.ObbBuiltinBones;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.context.interaction.ObbInteractionContext;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.event.BoneHitboxEvents;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.server.sync.contact.ServerObbContactStore;
import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/**
 * CN: 把原版服务端攻击、交互和投射物命中关联到客户端 OBB 语义，不重复执行原版行为。
 * EN: Correlates vanilla server attacks, interactions, and projectile hits with client OBB semantics without replaying vanilla behavior.
 */
public final class BoneHitboxServerHooks {
    private BoneHitboxServerHooks() {
    }

    public static void handlePlayerAttack(Player player, Entity target) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof BoneHitboxEntity)) {
            return;
        }
        ServerPartSelectionStore.selectedBone(serverPlayer, target)
                .ifPresent(hurtBone -> BoneHitboxAttackBoxHooks.handleVanillaAttack(
                        player,
                        target,
                        heldItemAttackBone(player.getMainHandItem()),
                        hurtBone,
                        VanillaDamageCapture.consume(player, target)));
    }

    public static void handleEntityAttack(Entity attacker, Entity target) {
        if (!(target instanceof BoneHitboxEntity)) {
            return;
        }
        ServerObbContactStore.findAttack(attacker, target).ifPresent(contact ->
                BoneHitboxAttackBoxHooks.handleVanillaAttack(
                        attacker,
                        target,
                        contact.attackBone(),
                        contact.hurtBone(),
                        VanillaDamageCapture.consume(attacker, target)));
    }

    public static InteractionResult handlePlayerInteract(Player player, InteractionHand hand, Entity target) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof BoneHitboxEntity hitboxEntity)) {
            return InteractionResult.PASS;
        }
        Optional<ObbBoneState> selected = ServerPartSelectionStore.selectedBone(serverPlayer, target);
        if (selected.isEmpty()) {
            return InteractionResult.PASS;
        }

        ObbInteractionContext context = new ObbInteractionContext(
                player,
                target,
                hand,
                selected.get(),
                ServerObbStore.animationState(target));
        InteractionResult before = hitboxEntity.kltyton_bedrock_runtime$beforeObbInteract(context);
        if (before != InteractionResult.PASS) {
            hitboxEntity.kltyton_bedrock_runtime$afterObbInteract(context, before);
            return before;
        }

        InteractionResult result = hitboxEntity.kltyton_bedrock_runtime$onObbInteract(context);
        InteractionResult playerResult = BoneHitboxEvents.firePlayerPartInteract(context);
        InteractionResult entityResult = BoneHitboxEvents.fireEntityPartInteract(context);
        if (result == InteractionResult.PASS && playerResult != InteractionResult.PASS) {
            result = playerResult;
        }
        if (result == InteractionResult.PASS && entityResult != InteractionResult.PASS) {
            result = entityResult;
        }
        hitboxEntity.kltyton_bedrock_runtime$afterObbInteract(context, result);
        return result;
    }

    public static void handleProjectileHit(Projectile projectile, Entity target) {
        if (projectile.level().isClientSide()) {
            return;
        }
        handleEntityAttack(projectile, target);
    }

    private static ObbBoneState heldItemAttackBone(ItemStack stack) {
        String name = "empty_hand";
        if (!stack.isEmpty()) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            name = itemId == null ? "unknown_item" : itemId.toString();
        }
        return ObbBoneState.synthetic(new ObbBoneKey(ObbBuiltinBones.HELD_ITEM_SOURCE, name, 0), ObbBoneAttribute.ATTACK);
    }
}
