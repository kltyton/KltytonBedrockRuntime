package com.kltyton.bedrockruntime.client.config;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;

/**
 * CN: OBB 调试渲染的客户端开关和按键。
 * EN: Client switches and key binding for OBB debug rendering.
 */
public final class BoneHitboxClientOptions {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Constants.id("kltyton_bedrock_runtime"));
    public static final KeyMapping HOLD_OBB_KEY = new KeyMapping(
            "key.kltyton_bedrock_runtime.hold_obb",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private BoneHitboxClientOptions() {
    }

    public static boolean shouldRenderCrosshairObb(Minecraft minecraft) {
        return BoneHitboxConfig.showCrosshairObb()
                || HOLD_OBB_KEY.isDown();
    }

    public static boolean shouldRenderDebugObbs(Minecraft minecraft) {
        return minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES);
    }
}
