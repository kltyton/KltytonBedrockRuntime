package com.kltyton.bedrockruntime.api.bone.builtin;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * CN: 库为没有可遍历视觉 cube 的原版对象提供的稳定合成骨骼键。
 * EN: Stable synthetic bone keys supplied for vanilla objects without traversable visual cubes.
 */
public final class ObbBuiltinBones {
    public static final String HELD_ITEM_SOURCE = "held_item";
    public static final String LEFT_HELD_ITEM_NAME = "left_item";
    public static final String RIGHT_HELD_ITEM_NAME = "right_item";

    private ObbBuiltinBones() {
    }

    /** CN: 人形实体左手物品的规范键。EN: Canonical key for a humanoid entity's left-hand item. */
    public static ObbBoneKey leftHeldItem(int cubeIndex) {
        return new ObbBoneKey(HELD_ITEM_SOURCE, LEFT_HELD_ITEM_NAME, cubeIndex);
    }

    /** CN: 人形实体右手物品的规范键。EN: Canonical key for a humanoid entity's right-hand item. */
    public static ObbBoneKey rightHeldItem(int cubeIndex) {
        return new ObbBoneKey(HELD_ITEM_SOURCE, RIGHT_HELD_ITEM_NAME, cubeIndex);
    }

    public static ObbBoneKey projectile(Entity projectile) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
        String name = typeId == null ? "unknown_projectile" : typeId.toString();
        return new ObbBoneKey("projectile", name, 0);
    }
}
