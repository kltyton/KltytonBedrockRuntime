package com.kltyton.bedrockruntime.server.state.player;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Holds saved player OBB data while no consuming mod has registered player bones. */
public interface PlayerObbSavedData {
    @Nullable CompoundTag kbr$inactivePlayerObb();

    void kbr$inactivePlayerObb(@Nullable CompoundTag data);
}
