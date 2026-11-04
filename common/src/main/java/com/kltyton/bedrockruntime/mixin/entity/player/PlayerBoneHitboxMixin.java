package com.kltyton.bedrockruntime.mixin.entity.player;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;

/**
 * CN: 让玩家也显式接入 BoneHitboxEntity；默认 OBB 开关语义适用于所有 OBB 功能。
 * EN: Opts players into BoneHitboxEntity; the default OBB switch applies to every OBB feature.
 */
@Mixin(Player.class)
public abstract class PlayerBoneHitboxMixin implements BoneHitboxEntity {
    @Override
    public void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(ObbBoneRegistrar.HUMAN_BASE);
    }
}
