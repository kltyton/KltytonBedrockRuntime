package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.kltyton.bedrockruntime.animation.graph.AnimationSampler;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.mixin.client.geckolib.GeckoLazyVariableAccessor;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import software.bernie.geckolib.core.molang.LazyVariable;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.molang.MolangQueries;
import software.bernie.geckolib.core.object.Axis;
import software.bernie.geckolib.model.GeoModel;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Pure GeckoLib4 animation sampling from baked keyframes. It never advances native controllers. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class Gecko4AnimationSampler implements AnimationSampler {
    private final GeoModel model;
    private final WeakReference<GeoAnimatable> animatable;

    public Gecko4AnimationSampler(GeoModel<?> model, GeoAnimatable animatable) {
        this.model = (GeoModel) Objects.requireNonNull(model, "model");
        this.animatable = new WeakReference<>(Objects.requireNonNull(animatable, "animatable"));
    }

    @Override
    public void sample(String name, double seconds, boolean loop, PoseBuffer target) {
        Objects.requireNonNull(target, "target");

        Animation animation = requireAnimation(name);
        double lengthTicks = animation.length();
        double ticks = seconds * 20.0;
        double localTicks;

        if (loop && lengthTicks > 0.0) {
            localTicks = ticks % lengthTicks;

            if (localTicks < 0.0) {
                localTicks += lengthTicks;
            }
        }
        else {
            localTicks = Math.max(0.0, Math.min(lengthTicks, ticks));
        }

        LazyVariable animTime = MolangParser.INSTANCE.getVariable(MolangQueries.ANIM_TIME);
        DoubleSupplier previousSupplier = ((GeckoLazyVariableAccessor) (Object) animTime)
                .kltyton_bedrock_runtime$getValueSupplier();

        double sampleSeconds = localTicks / 20.0;
        animTime.set(() -> sampleSeconds);

        try {
            target.resetToBind();

            SkeletonBinding binding = target.binding();

            for (BoneAnimation boneAnimation : animation.boneAnimations()) {
                int index = binding.indexOf(boneAnimation.boneName());

                if (index < 0) {
                    continue;
                }

                KeyframeStack<Keyframe<IValue>> rotation = boneAnimation.rotationKeyFrames();
                KeyframeStack<Keyframe<IValue>> position = boneAnimation.positionKeyFrames();
                KeyframeStack<Keyframe<IValue>> scale = boneAnimation.scaleKeyFrames();

                if (present(rotation)) {
                    SkeletonBinding.Bone bone = binding.bone(index);

                    target.setRotation(index,
                            bone.bindRotX() + evaluate(rotation.xKeyframes(), localTicks, true, Axis.X),
                            bone.bindRotY() + evaluate(rotation.yKeyframes(), localTicks, true, Axis.Y),
                            bone.bindRotZ() + evaluate(rotation.zKeyframes(), localTicks, true, Axis.Z));
                }

                if (present(position)) {
                    target.setTranslation(index,
                            evaluate(position.xKeyframes(), localTicks, false, Axis.X),
                            evaluate(position.yKeyframes(), localTicks, false, Axis.Y),
                            evaluate(position.zKeyframes(), localTicks, false, Axis.Z));
                }

                if (present(scale)) {
                    target.setScale(index,
                            evaluate(scale.xKeyframes(), localTicks, false, Axis.X),
                            evaluate(scale.yKeyframes(), localTicks, false, Axis.Y),
                            evaluate(scale.zKeyframes(), localTicks, false, Axis.Z));
                }
            }
        }
        finally {
            animTime.set(previousSupplier);
        }
    }

    @Override
    public double duration(String animation) {
        return requireAnimation(animation).length() / 20.0;
    }

    private Animation requireAnimation(String name) {
        GeoAnimatable animatable = this.animatable.get();

        if (animatable == null) {
            throw new IllegalStateException("GeckoLib animatable was released while sampling animation '" + name + "'");
        }

        Animation animation = this.model.getAnimation(animatable, name);

        if (animation == null) {
            throw new IllegalArgumentException("Animation graph references missing GeckoLib animation '" + name + "'");
        }

        return animation;
    }

    private static boolean present(KeyframeStack<?> stack) {
        return !stack.xKeyframes().isEmpty() && !stack.yKeyframes().isEmpty() && !stack.zKeyframes().isEmpty();
    }

    private static float evaluate(List<Keyframe<IValue>> frames, double tick, boolean rotation, Axis axis) {
        if (frames.isEmpty()) {
            return 0.0F;
        }

        double total = 0.0;

        for (Keyframe<IValue> frame : frames) {
            double length = frame.length();

            total += length;

            if (total > tick) {
                double currentTick = tick - (total - length);
                double startValue = frame.startValue().get();
                double endValue = frame.endValue().get();

                if (rotation) {
                    if (!(frame.startValue() instanceof Constant)) {
                        startValue = rotationRadians(startValue, axis);
                    }

                    if (!(frame.endValue() instanceof Constant)) {
                        endValue = rotationRadians(endValue, axis);
                    }
                }

                return (float) EasingType.lerpWithOverride(
                        new AnimationPoint(frame, currentTick, length, startValue, endValue), null);
            }
        }

        Keyframe<IValue> last = frames.get(frames.size() - 1);
        double endValue = last.endValue().get();

        if (rotation && !(last.endValue() instanceof Constant)) {
            endValue = rotationRadians(endValue, axis);
        }

        return (float) endValue;
    }

    private static double rotationRadians(double value, Axis axis) {
        double radians = Math.toRadians(value);

        if (axis == Axis.X || axis == Axis.Y) {
            radians = -radians;
        }

        return radians;
    }
}
