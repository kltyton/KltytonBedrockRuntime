package com.kltyton.bedrockruntime.example.playerobb.mixin;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.registrar.ObbBoneRegistrar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerBoneHitboxMixin implements BoneHitboxEntity {
    @Override
    public void kltyton_bedrock_runtime$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(ObbBoneRegistrar.HUMAN_BASE);
    }
}
