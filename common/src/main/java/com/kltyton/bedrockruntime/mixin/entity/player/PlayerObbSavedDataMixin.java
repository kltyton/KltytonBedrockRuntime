package com.kltyton.bedrockruntime.mixin.entity.player;

import com.kltyton.bedrockruntime.server.state.player.PlayerObbSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Keeps old player OBB data attached to the player without registering geometry. */
@Mixin(Player.class)
abstract class PlayerObbSavedDataMixin implements PlayerObbSavedData {
    @Unique private @Nullable CompoundTag kbr$inactiveObb;

    @Override
    public @Nullable CompoundTag kbr$inactivePlayerObb() {
        return kbr$inactiveObb;
    }

    @Override
    public void kbr$inactivePlayerObb(@Nullable CompoundTag data) {
        kbr$inactiveObb = data;
    }
}
