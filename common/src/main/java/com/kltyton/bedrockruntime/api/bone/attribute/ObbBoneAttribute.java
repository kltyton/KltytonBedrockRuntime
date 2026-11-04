package com.kltyton.bedrockruntime.api.bone.attribute;

import java.util.EnumSet;
import java.util.Set;

/**
 * CN: 可同时附加到同一个 OBB 骨骼上的行为属性；这些值不是互斥的碰撞箱类型。
 * EN: Composable behavior attributes attached to one OBB bone; these are not mutually exclusive box types.
 */
public enum ObbBoneAttribute {
    /** CN: 可作为攻击来源。EN: Can act as an attack source. */
    ATTACK,
    /** CN: 可作为受击目标。EN: Can act as a hurt target. */
    HURT,
    /** CN: 启用软/硬物理响应；所有已注册 OBB 均会产生通用接触事件。EN: Enables soft/hard physical response; every registered OBB emits generic contact events. */
    COLLISION;

    public static int toMask(Set<ObbBoneAttribute> attributes) {
        int mask = 0;
        if (attributes != null) {
            for (ObbBoneAttribute attribute : attributes) {
                mask |= 1 << attribute.ordinal();
            }
        }
        return mask;
    }

    public static Set<ObbBoneAttribute> fromMask(int mask) {
        EnumSet<ObbBoneAttribute> attributes = EnumSet.noneOf(ObbBoneAttribute.class);
        for (ObbBoneAttribute attribute : values()) {
            if ((mask & 1 << attribute.ordinal()) != 0) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }
}
