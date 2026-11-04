package com.kltyton.bedrockruntime.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BillboardQuadDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

/** Carries billboard geometry while retaining native quad counts, buffers and submission. */
@Mixin(QuadParticleRenderState.class)
public abstract class BillboardQuadRenderStateMixin implements BillboardQuadDimensions.Access {
    @Shadow @Final
    private Map<SingleQuadParticle.Layer, ?> particles;

    @Override
    public void kltyton_bedrock_runtime$addBillboard(SingleQuadParticle.Layer layer, float x, float y, float z,
                                          Quaternionf orientation, float width, float height,
                                          float u0, float u1, float v0, float v1, int color, int light) {
        kltyton_bedrock_runtime$addFace(layer, x, y, z, orientation, width, height, u0, u1, v0, v1, color, light);
        if (layer.pipeline().isCull()) {
            Quaternionf back = new Quaternionf(orientation).rotateY((float) Math.PI);
            kltyton_bedrock_runtime$addFace(layer, x, y, z, back, width, height, u1, u0, v0, v1, color, light);
        }
    }

    @Override
    public void kltyton_bedrock_runtime$addOrientedBillboard(SingleQuadParticle.Layer layer, float x, float y, float z,
                                                   Matrix3fc basis, float width, float height,
                                                   float u0, float u1, float v0, float v1, int color, int light) {
        kltyton_bedrock_runtime$addOrientedFace(layer, x, y, z, basis, false, width, height, u0, u1, v0, v1, color, light);
        if (layer.pipeline().isCull()) {
            kltyton_bedrock_runtime$addOrientedFace(layer, x, y, z, basis, true, width, height, u1, u0, v0, v1, color, light);
        }
    }

    @Unique
    private void kltyton_bedrock_runtime$addOrientedFace(SingleQuadParticle.Layer layer, float x, float y, float z,
                                               Matrix3fc basis, boolean backFace, float width, float height,
                                               float u0, float u1, float v0, float v1, int color, int light) {
        ((QuadParticleRenderState) (Object) this).add(layer, x, y, z,
                0.0F, 0.0F, 0.0F, 1.0F, width, u0, u1, v0, v1, color, light);
        BillboardQuadDimensions.StorageAccess storage = (BillboardQuadDimensions.StorageAccess) particles.get(layer);
        if (width != height) storage.kltyton_bedrock_runtime$setBillboardHeight(height);
        storage.kltyton_bedrock_runtime$setBillboardBasis(basis, backFace);
    }

    @Unique
    private void kltyton_bedrock_runtime$addFace(SingleQuadParticle.Layer layer, float x, float y, float z,
                                      Quaternionf orientation, float width, float height,
                                      float u0, float u1, float v0, float v1, int color, int light) {
        ((QuadParticleRenderState) (Object) this).add(layer, x, y, z,
                orientation.x, orientation.y, orientation.z, orientation.w, width,
                u0, u1, v0, v1, color, light);
        if (width != height) {
            ((BillboardQuadDimensions.StorageAccess) particles.get(layer)).kltyton_bedrock_runtime$setBillboardHeight(height);
        }
    }

    @ModifyVariable(method = "renderRotatedQuad", at = @At("HEAD"), argsOnly = true, ordinal = 7)
    private float kltyton_bedrock_runtime$selectBillboardDimensions(float width) {
        return BillboardQuadDimensions.selectNext(width);
    }

    @ModifyArg(method = "renderVertex",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;<init>(FFF)V"), index = 1)
    private float kltyton_bedrock_runtime$scaleBillboardHeight(float y, @Local(argsOnly = true, ordinal = 5) float width) {
        return BillboardQuadDimensions.scaleY(y, width);
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderVertex",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;mul(F)Lorg/joml/Vector3f;"))
    private Vector3f kltyton_bedrock_runtime$applyBillboardBasis(Vector3f point,
                                                       @Local(argsOnly = true) Quaternionf rotation,
                                                       @Local(argsOnly = true, ordinal = 3) float horizontal,
                                                       @Local(argsOnly = true, ordinal = 4) float vertical,
                                                       @Local(argsOnly = true, ordinal = 5) float width) {
        return BillboardQuadDimensions.applyBasis(point, rotation, horizontal, vertical, width);
    }

    /** Keeps geometry aligned with native storage slots, including clear and buffer growth. */
    @Mixin(targets = "net.minecraft.client.renderer.state.level.QuadParticleRenderState$Storage")
    public abstract static class StorageMixin implements BillboardQuadDimensions.StorageAccess {
        @Shadow private int currentParticleIndex;
        @Shadow private int capacity;
        @Unique private @Nullable float[] kltyton_bedrock_runtime$heights;
        @Unique private @Nullable BitSet kltyton_bedrock_runtime$rectangles;
        @Unique private @Nullable float[] kltyton_bedrock_runtime$axes;
        @Unique private @Nullable BitSet kltyton_bedrock_runtime$oriented;

        @Override
        public void kltyton_bedrock_runtime$setBillboardHeight(float height) {
            BitSet rectangles = kltyton_bedrock_runtime$rectangles;
            if (rectangles == null) {
                rectangles = new BitSet(capacity);
                kltyton_bedrock_runtime$rectangles = rectangles;
                kltyton_bedrock_runtime$heights = new float[capacity];
            }
            int index = currentParticleIndex - 1;
            kltyton_bedrock_runtime$heights[index] = height;
            rectangles.set(index);
        }

        @Override
        public void kltyton_bedrock_runtime$setBillboardBasis(Matrix3fc basis, boolean backFace) {
            BitSet oriented = kltyton_bedrock_runtime$oriented;
            if (oriented == null) {
                oriented = new BitSet(capacity);
                kltyton_bedrock_runtime$oriented = oriented;
                kltyton_bedrock_runtime$axes = new float[capacity * 6];
            }
            int slot = currentParticleIndex - 1;
            int index = slot * 6;
            float sign = backFace ? -1.0F : 1.0F;
            float[] axes = kltyton_bedrock_runtime$axes;
            axes[index] = basis.m00() * sign;
            axes[index + 1] = basis.m01() * sign;
            axes[index + 2] = basis.m02() * sign;
            axes[index + 3] = basis.m10();
            axes[index + 4] = basis.m11();
            axes[index + 5] = basis.m12();
            oriented.set(slot);
        }

        @Inject(method = "add", at = @At("TAIL"))
        private void kltyton_bedrock_runtime$resetBillboardSlot(CallbackInfo ci) {
            BitSet rectangles = kltyton_bedrock_runtime$rectangles;
            if (rectangles != null) rectangles.clear(currentParticleIndex - 1);
            BitSet oriented = kltyton_bedrock_runtime$oriented;
            if (oriented != null) oriented.clear(currentParticleIndex - 1);
        }

        @Inject(method = "grow", at = @At("TAIL"))
        private void kltyton_bedrock_runtime$growBillboardSlots(CallbackInfo ci) {
            float[] heights = kltyton_bedrock_runtime$heights;
            if (heights != null) kltyton_bedrock_runtime$heights = Arrays.copyOf(heights, capacity);
            float[] axes = kltyton_bedrock_runtime$axes;
            if (axes != null) kltyton_bedrock_runtime$axes = Arrays.copyOf(axes, capacity * 6);
        }

        @Inject(method = "clear", at = @At("TAIL"))
        private void kltyton_bedrock_runtime$clearBillboardSlots(CallbackInfo ci) {
            BitSet rectangles = kltyton_bedrock_runtime$rectangles;
            if (rectangles != null) rectangles.clear();
            BitSet oriented = kltyton_bedrock_runtime$oriented;
            if (oriented != null) oriented.clear();
        }

        @WrapMethod(method = "forEachParticle")
        private void kltyton_bedrock_runtime$withBillboardDimensions(QuadParticleRenderState.ParticleConsumer consumer,
                                                          Operation<Void> original) {
            BitSet rectangles = kltyton_bedrock_runtime$rectangles;
            BitSet oriented = kltyton_bedrock_runtime$oriented;
            if ((rectangles == null || rectangles.isEmpty()) && (oriented == null || oriented.isEmpty())
                    && !BillboardQuadDimensions.isActive()) {
                original.call(consumer);
                return;
            }
            BillboardQuadDimensions.enter(kltyton_bedrock_runtime$heights, rectangles, kltyton_bedrock_runtime$axes, oriented);
            try {
                original.call(consumer);
            } finally {
                BillboardQuadDimensions.exit();
            }
        }
    }
}
