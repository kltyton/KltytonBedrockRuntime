package com.kltyton.bedrockruntime.client.selection.service;

import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class BonePartSelectionClient extends BonePartSelectionLogic {
    private BonePartSelectionClient() { }

    public static boolean shouldSuppressFullEntityRed(EntityRenderState state) {
        if (!shouldReplaceFullRed(state)) {
            return false;
        }
        return shouldSuppressFullEntityRed(VisualPartId.ownerKey(state));
    }

    private static boolean shouldReplaceFullRed(EntityRenderState state) {
        String ownerKey = VisualPartId.ownerKey(state);
        if (!ownerKey.startsWith("entity:")) {
            return false;
        }

        try {
            return shouldReplaceFullRed(entityById(Integer.parseInt(ownerKey.substring("entity:".length()))));
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
