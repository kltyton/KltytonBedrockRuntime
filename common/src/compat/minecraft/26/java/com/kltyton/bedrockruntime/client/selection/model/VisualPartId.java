package com.kltyton.bedrockruntime.client.selection.model;

import com.kltyton.bedrockruntime.Constants;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * CN: 渲染视觉 cube 的稳定身份。
 * EN: Stable identity for a rendered visual cube.
 */
public record VisualPartId(String source, String ownerKey, String partName, int cubeIndex) {

    public static VisualPartId gecko(long entityId, String boneName, int cubeIndex) {
        return new VisualPartId("gecko", ownerKey(entityId), boneName, cubeIndex);
    }

    public static VisualPartId vanilla(EntityRenderState state, String partName, int cubeIndex) {
        return new VisualPartId("vanilla", ownerKey(state), partName, cubeIndex);
    }

    public static String ownerKey(Entity entity) {
        return ownerKey(entity.getId());
    }

    public static String ownerKey(EntityRenderState state) {
        int entityId = state instanceof VisualPartEntityRenderState visualState ? visualState.kltyton_bedrock_runtime$getEntityId() : -1;
        if (entityId >= 0) {
            return ownerKey(entityId);
        }

        Identifier typeId = state.entityType == null ? Constants.id("unknown_entity") : BuiltInRegistries.ENTITY_TYPE.getKey(state.entityType);
        return typeId + "@" + quantize(state.x) + "," + quantize(state.y) + "," + quantize(state.z);
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

    private static int quantize(double value) {
        return Mth.floor(value * 4.0);
    }
}
