package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPlayerPersistentData {
    String TAG_KEY = "KltytonBedrockRuntimePersistentData";

    CompoundTag kltyton_bedrock_runtime$getPersistentData();

    static IPlayerPersistentData of(Player player) {
        return (IPlayerPersistentData) player;
    }
}
