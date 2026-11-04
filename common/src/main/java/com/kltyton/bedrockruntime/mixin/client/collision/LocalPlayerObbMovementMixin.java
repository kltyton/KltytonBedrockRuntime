package com.kltyton.bedrockruntime.mixin.client.collision;

import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CN: 在 Entity.move 内部的原版 collide 结果上追加连续 OBB 求解，使最终位移仍由原版流程应用。
 * EN: Adds continuous OBB resolution to vanilla collide inside Entity.move so vanilla still applies the final displacement.
 */
@Mixin(Entity.class)
public abstract class LocalPlayerObbMovementMixin {
    @Shadow
    private Vec3 collide(Vec3 movement) {
        throw new AssertionError();
    }

    @Redirect(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 kltyton_bedrock_runtime$resolveMovementAgainstObbs(Entity entity, Vec3 requestedMovement) {
        Vec3 vanillaMovement = collide(requestedMovement);
        return BonePartSelectionClient.resolveLocalPlayerMovement(entity, requestedMovement, vanillaMovement);
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void kltyton_bedrock_runtime$finishObbSupportedMovement(MoverType moverType, Vec3 movement,
            CallbackInfo callbackInfo) {
        BonePartSelectionClient.finishLocalPlayerMovement((Entity) (Object) this, movement);
    }
}
