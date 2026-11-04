package com.kltyton.bedrockruntime.api.registration.registrar;

import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.registration.definition.ObbBoneDefinition;
import com.kltyton.bedrockruntime.api.registration.selector.ObbBoneSelector;
import com.kltyton.bedrockruntime.api.registration.selector.ObbBoneSelectors;

import java.util.ArrayList;
import java.util.List;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;

/**
 * CN: 由 BoneHitboxEntity 的强制注册方法填充的实体级骨骼注册器。
 * EN: Entity-level bone registrar populated by BoneHitboxEntity's required registration method.
 */
public final class ObbBoneRegistrar {
    public static final ObbBoneSelector ALL = ObbBoneSelectors.ALL;
    public static final ObbBoneSelector BASE = ObbBoneSelectors.BASE;
    /** CN: BASE 加左右手持物品的人形预设；不会自动注册到任何实体。EN: Humanoid preset containing BASE plus both held items; never auto-registered. */
    public static final ObbBoneSelector HUMAN_BASE = ObbBoneSelectors.HUMAN_BASE;

    private final List<ObbBoneRegistration> registrations = new ArrayList<>();

    public ObbBoneRegistration register(ObbBoneSelector selector) {
        ObbBoneRegistration registration = new ObbBoneRegistration(selector);
        registrations.add(registration);
        return registration;
    }

    public ObbBoneRegistration register(String boneName) {
        return register(ObbBoneSelectors.named(boneName));
    }

    public ObbBoneRegistration register(String source, String boneName) {
        return register(ObbBoneSelectors.named(source, boneName));
    }

    public ObbBoneRegistration register(ObbBoneKey key) {
        return register(ObbBoneSelectors.exact(key));
    }

    public List<ObbBoneDefinition> definitions() {
        return registrations.stream().map(ObbBoneRegistration::freeze).toList();
    }
}
