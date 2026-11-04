package com.kltyton.bedrockruntime.api.registration.selector;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;

/**
 * CN: 注册阶段用于匹配视觉模型骨骼/cube 的选择器。
 * EN: Registration-time selector for visual-model bones/cubes.
 */
@FunctionalInterface
public interface ObbBoneSelector {
    boolean matches(ObbBoneKey key);
}
