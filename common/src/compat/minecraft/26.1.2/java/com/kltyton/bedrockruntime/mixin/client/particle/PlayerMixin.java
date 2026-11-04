package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.kltyton.bedrockruntime.client.particle.access.IPlayerPersistentData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerPersistentData {
    @Unique
    private CompoundTag kltyton_bedrock_runtime$persistentData = new CompoundTag();

    @Override
    public CompoundTag kltyton_bedrock_runtime$getPersistentData() {
        return kltyton_bedrock_runtime$persistentData;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$readPersistentData(ValueInput input, CallbackInfo ci) {
        this.kltyton_bedrock_runtime$persistentData = input.read(TAG_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$writePersistentData(ValueOutput output, CallbackInfo ci) {
        if (!kltyton_bedrock_runtime$persistentData.isEmpty()) {
            output.store(TAG_KEY, CompoundTag.CODEC, kltyton_bedrock_runtime$persistentData);
        }
    }
}
