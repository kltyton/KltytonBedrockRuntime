package com.kltyton.bedrockruntime.api.bone.collision;

/**
 * CN: 带 COLLISION 属性骨骼的独立 OBB 物理响应模式，不映射为原版 VoxelShape。
 * EN: Independent OBB physical response for COLLISION bones; it is not mapped to a vanilla VoxelShape.
 */
public enum ObbCollisionMode {
    NONE,
    SOFT,
    HARD;

    public static ObbCollisionMode strongest(ObbCollisionMode first, ObbCollisionMode second) {
        ObbCollisionMode safeFirst = first == null ? NONE : first;
        ObbCollisionMode safeSecond = second == null ? NONE : second;
        return safeFirst.ordinal() >= safeSecond.ordinal() ? safeFirst : safeSecond;
    }
}
