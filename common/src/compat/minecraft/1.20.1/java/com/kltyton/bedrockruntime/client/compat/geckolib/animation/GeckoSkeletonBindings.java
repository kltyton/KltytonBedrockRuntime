package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Shared GeckoLib4 bindings, weakly keyed by the actual baked model instance. */
public final class GeckoSkeletonBindings {
    private static final Cache<BakedGeoModel, SkeletonBinding> BINDINGS = CacheBuilder.newBuilder().weakKeys().build();
    private static final AtomicLong GENERATIONS = new AtomicLong();

    private GeckoSkeletonBindings() {
    }

    public static SkeletonBinding get(BakedGeoModel model, String resource) {
        SkeletonBinding existing = BINDINGS.getIfPresent(model);

        if (existing != null && existing.resource().equals(resource)) {
            return existing;
        }

        List<SkeletonBinding.Bone> bones = new ArrayList<>();

        for (GeoBone root : model.topLevelBones()) {
            append(root, null, bones);
        }

        SkeletonBinding binding = new SkeletonBinding(resource, GENERATIONS.incrementAndGet(), bones);

        BINDINGS.put(model, binding);

        return binding;
    }

    private static void append(GeoBone bone, String parent, List<SkeletonBinding.Bone> bones) {
        BoneSnapshot initial = bone.getInitialSnapshot();

        if (initial == null) {
            bone.saveInitialSnapshot();
            initial = bone.getInitialSnapshot();
        }

        float rotX = initial == null ? bone.getRotX() : initial.getRotX();
        float rotY = initial == null ? bone.getRotY() : initial.getRotY();
        float rotZ = initial == null ? bone.getRotZ() : initial.getRotZ();

        bones.add(new SkeletonBinding.Bone(bone.getName(), parent, bone.getPivotX(), bone.getPivotY(),
                bone.getPivotZ(), rotX, rotY, rotZ));

        for (GeoBone child : bone.getChildBones()) {
            append(child, bone.getName(), bones);
        }
    }
}
