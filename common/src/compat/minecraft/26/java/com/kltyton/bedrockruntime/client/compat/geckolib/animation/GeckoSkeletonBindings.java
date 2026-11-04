package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.client.compat.geckolib.model.EnhancedBakedGeoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** Shares immutable bindings without retaining models across resource reloads. */
public final class GeckoSkeletonBindings {
    private static final Cache<BakedGeoModel, SkeletonBinding> BINDINGS = CacheBuilder.newBuilder().weakKeys().build();
    private static final AtomicLong GENERATIONS = new AtomicLong();

    private GeckoSkeletonBindings() { }

    public static SkeletonBinding get(BakedGeoModel model, String resource) {
        if (model instanceof EnhancedBakedGeoModel enhanced) {
            return enhanced.skeleton();
        }
        try {
            return BINDINGS.get(model, () -> {
                List<SkeletonBinding.Bone> bones = new ArrayList<>();
                for (GeoBone root : model.topLevelBones()) {
                    append(root, null, bones);
                }
                return new SkeletonBinding(resource, GENERATIONS.incrementAndGet(), bones);
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Cannot bind GeckoLib model " + resource, exception.getCause());
        }
    }

    private static void append(GeoBone bone, String parent, List<SkeletonBinding.Bone> bones) {
        bones.add(new SkeletonBinding.Bone(bone.name(), parent, bone.pivotX(), bone.pivotY(), bone.pivotZ(),
                bone.baseRotX(), bone.baseRotY(), bone.baseRotZ()));
        for (GeoBone child : bone.children()) {
            append(child, bone.name(), bones);
        }
    }
}
