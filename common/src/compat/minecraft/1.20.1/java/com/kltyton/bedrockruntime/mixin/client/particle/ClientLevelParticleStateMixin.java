package com.kltyton.bedrockruntime.mixin.client.particle;

import com.kltyton.bedrockruntime.client.particle.molang.MolangEntityStateInbox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies initial Molang entity state before the entity is added to the client level. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelParticleStateMixin {
    @Inject(method = "putNonPlayerEntity", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$applyInitialEntityState(int id, Entity entity, CallbackInfo callback) {
        MolangEntityStateInbox.applyInitial((ClientLevel) (Object) this, entity);
    }
}
