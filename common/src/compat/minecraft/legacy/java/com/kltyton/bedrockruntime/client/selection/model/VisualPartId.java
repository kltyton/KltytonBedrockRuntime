package com.kltyton.bedrockruntime.client.selection.model;


import net.minecraft.world.entity.Entity;

/**
 * CN: 渲染视觉 cube 的稳定身份。
 * EN: Stable identity for a rendered visual cube.
 */
public record VisualPartId(String source, String ownerKey, String partName, int cubeIndex) {

    public static VisualPartId gecko(long entityId, String boneName, int cubeIndex) {
        return new VisualPartId("gecko", ownerKey(entityId), boneName, cubeIndex);
    }

    public static VisualPartId vanilla(Entity state, String partName, int cubeIndex) {
        return new VisualPartId("vanilla", ownerKey(state), partName, cubeIndex);
    }

    public static String ownerKey(Entity entity) {
        return ownerKey(entity.getId());
    }

    private static String ownerKey(long entityId) {
        return "entity:" + entityId;
    }

    public String displayName() {
        return source + ":" + partName + "[" + cubeIndex + "]";
    }

    public int ownerEntityId() {
        if (!ownerKey.startsWith("entity:")) {
            return -1;
        }

        try {
            return Integer.parseInt(ownerKey.substring("entity:".length()));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

}
