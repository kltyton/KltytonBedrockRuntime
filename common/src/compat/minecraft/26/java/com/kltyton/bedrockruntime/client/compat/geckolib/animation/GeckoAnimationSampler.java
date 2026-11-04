package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.cache.animation.KeyframeStack;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.math.value.Variable;
import com.geckolib.renderer.base.GeoRenderState;
import com.kltyton.bedrockruntime.animation.graph.AnimationSampler;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** GeckoLib 5.5.x sampling boundary. It never advances a controller or emits keyframe events. */
public final class GeckoAnimationSampler implements AnimationSampler {
    private final BakedGeoModel model;
    private final Map<PoseBuffer, Cursor> cursors = new IdentityHashMap<>();
    private Map<String, Animation> animations = Map.of();
    private GeoRenderState renderState;
    private Reference2DoubleMap<Variable> queries;

    public GeckoAnimationSampler(BakedGeoModel model) {
        this.model = model;
    }

    public void capture(Map<String, Animation> animations, GeoRenderState renderState, Reference2DoubleMap<Variable> queries) {
        this.animations = animations;
        this.renderState = renderState;
        this.queries = queries;
    }

    public void releaseCapture() {
        this.animations = Map.of();
        this.renderState = null;
        this.queries = null;
    }

    @Override
    public void sample(String name, double seconds, boolean loop, PoseBuffer target) {
        Animation animation = requireAnimation(name);
        double length = animation.length();
        double time = loop && length > 0 ? seconds - Math.floor(seconds / length) * length
                : Math.max(0, Math.min(length, seconds));
        Cursor cursor = cursors.computeIfAbsent(target, ignored -> new Cursor());
        if (cursor.animation != animation) {
            cursor.animation = animation;
            cursor.point = AnimationPoint.createFor(animation, null, animation.loopType(), time);
            cursor.snapshots.clear();
        } else {
            cursor.point = cursor.point.createNext(time);
        }
        for (BoneSnapshot snapshot : cursor.snapshots.values()) {
            snapshot.setTranslation(0, 0, 0).setRotation(0, 0, 0).setScale(1, 1, 1);
        }
        ControllerState state = new ControllerState(cursor.point, null, -1, 0, false, null, renderState, queries);
        AnimationProcessor.createBoneSnapshots(state, bone -> {
            if (!cursor.snapshots.containsKey(bone)) {
                model.getBone(bone).ifPresent(value -> cursor.snapshots.put(bone, BoneSnapshot.create(value)));
            }
            return Optional.ofNullable(cursor.snapshots.get(bone));
        });
        target.resetToBind();
        for (BoneAnimation bone : animation.boneAnimations()) {
            int index = target.binding().indexOf(bone.boneName());
            BoneSnapshot snapshot = cursor.snapshots.get(bone.boneName());
            if (index < 0 || snapshot == null) {
                continue;
            }
            if (present(bone.positionKeyFrames())) {
                target.setTranslation(index, snapshot.getTranslateX(), snapshot.getTranslateY(), snapshot.getTranslateZ());
            }
            if (present(bone.rotationKeyFrames())) {
                var binding = target.binding().bone(index);
                target.setRotation(index, binding.bindRotX() + snapshot.getRotX(), binding.bindRotY() + snapshot.getRotY(),
                        binding.bindRotZ() + snapshot.getRotZ());
            }
            if (present(bone.scaleKeyFrames())) {
                target.setScale(index, snapshot.getScaleX(), snapshot.getScaleY(), snapshot.getScaleZ());
            }
        }
    }

    public double duration(String animation) { return requireAnimation(animation).length(); }

    private Animation requireAnimation(String name) {
        Animation animation = animations.get(name);
        if (animation == null) {
            throw new IllegalArgumentException("Animation graph references missing GeckoLib animation '" + name + "'");
        }
        return animation;
    }

    private static boolean present(KeyframeStack channel) {
        return channel.xKeyframes().length != 0 || channel.yKeyframes().length != 0 || channel.zKeyframes().length != 0;
    }

    private static final class Cursor {
        Animation animation;
        AnimationPoint point;
        final Map<String, BoneSnapshot> snapshots = new HashMap<>();
    }
}
