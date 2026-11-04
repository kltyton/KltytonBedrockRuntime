package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.client.particle.runtime.screen.PreviewParticleScope;
import com.kltyton.bedrockruntime.client.particle.molang.MolangEntityStateInbox;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Redirects vanilla per-block animate ticks to ParticleStorm attached emitters when a mod has
/// registered an attachment for the block state through AttachEmitterToBlockEvent.
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void kltyton_bedrock_runtime$applyInitialEntityState(Entity entity, CallbackInfo ci) {
        MolangEntityStateInbox.applyInitial((ClientLevel) (Object) this, entity);
    }

    @ModifyExpressionValue(method = "doAddParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getMainCamera()Lnet/minecraft/client/Camera;"))
    private Camera kltyton_bedrock_runtime$previewCamera(Camera original) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context != null && context.level() == (Object) this && context.camera() != null
                ? context.camera() : original;
    }

    @WrapOperation(method = "doAddParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"))
    private double kltyton_bedrock_runtime$previewDistance(Vec3 position, double x, double y, double z, Operation<Double> original) {
        double distance = original.call(position, x, y, z);
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        return context != null && context.level() == (Object) this ? 0.0 : distance;
    }

    @WrapOperation(method = "doAddParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"))
    private @Nullable Particle kltyton_bedrock_runtime$previewParticleEngine(ParticleEngine engine, ParticleOptions particle,
            double x, double y, double z, double xd, double yd, double zd, Operation<Particle> original) {
        PreviewParticleScope.Context context = PreviewParticleScope.current();
        if (context != null && context.level() == (Object) this
                && context.manager().isConstructingPreviewNativeEngine()) {
            // ClientLevel discards createParticle's return value; replay the call after registration.
            context.manager().deferPreviewParticleAction(() -> original.call(
                    context.manager().previewNativeEngine(context.level()), particle, x, y, z, xd, yd, zd));
            return null;
        }
        ParticleEngine target = context != null && context.level() == (Object) this
                ? context.manager().previewNativeEngine(context.level()) : engine;
        return original.call(target, particle, x, y, z, xd, yd, zd);
    }

    @WrapWithCondition(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean kltyton_bedrock_runtime$takeOver(Block instance, BlockState state, Level level, BlockPos pos, RandomSource random) {
        return EmitterAttachHandler.attachTo(instance, state, level, pos);
    }
}