package com.kltyton.bedrockruntime.mixin.entity.player;

import com.kltyton.bedrockruntime.server.state.player.PlayerObbSavedData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries inactive OBB save data across the native player replacement. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerObbSavedDataMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void kbr$restoreInactiveObb(ServerPlayer previous, boolean alive, CallbackInfo callback) {
        var saved = ((PlayerObbSavedData) previous).kbr$inactivePlayerObb();
        if (saved != null) ((PlayerObbSavedData) this).kbr$inactivePlayerObb(saved.copy());
    }
}
