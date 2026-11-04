package com.kltyton.bedrockruntime.client.skill.keyframe;

import java.util.HashSet;
import java.util.Set;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.geckolib.skill.entity.GeoKeyframeSkillEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * CN: 在客户端逻辑 tick 中推进已追踪技能实体的 GeckoLib 控制器，不提交任何实际绘制。
 * EN: Advances tracked skill entities' GeckoLib controllers on client logic ticks without submitting a draw.
 */
public final class GeoKeyframeSkillClientTicker {
    private static final Set<EntityType<?>> WARNED_ENTITY_TYPES = new HashSet<>();

    private static ClientLevel lastLevel;
    private static long lastGameTime = Long.MIN_VALUE;

    private GeoKeyframeSkillClientTicker() {
    }

    /**
     * CN: 对每个客户端已追踪且显式实现接口的实体提取一次零 partial-tick 渲染状态。
     * 该公开原版入口会运行 GeckoLib 的控制器与 marker 时间轴，但不会进入 submit/render 阶段。
     * EN: Extracts one zero-partial-tick render state for every tracked entity that explicitly implements the contract.
     * This public vanilla entrypoint runs GeckoLib's controller and marker timeline without entering submit/render.
     */
    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        long gameTime = level.getGameTime();
        if (level == lastLevel && gameTime == lastGameTime) {
            return;
        }
        if (level != lastLevel) {
            WARNED_ENTITY_TYPES.clear();
            lastLevel = level;
        }
        lastGameTime = gameTime;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || !(entity instanceof com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity)) {
                continue;
            }

            try {
                // GeckoLib 5.5.3 reserves partialTick=1 for frozen animation sampling.
                float partialTick = level.tickRateManager().isEntityFrozen(entity) ? 1.0F : 0.0F;
                var renderState = minecraft.getEntityRenderDispatcher().extractEntity(entity, partialTick);
                if (com.kltyton.bedrockruntime.compat.geckolib.GeckoLibCompat.isLoaded()) {
                    var states = com.kltyton.bedrockruntime.client.compat.geckolib.skill.GeoAnimationSnapshots.capture(renderState);
                    com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient.recordAnimations(entity.getId(), states);
                    ((com.kltyton.bedrockruntime.api.geckolib.entity.GeoBoneHitboxEntity) entity).kltyton_bedrock_runtime$geoUpdateAnimationStates(states);
                }
            } catch (RuntimeException exception) {
                if (WARNED_ENTITY_TYPES.add(entity.getType())) {
                    Constants.LOG.warn(
                            "Unable to tick off-screen GeckoLib keyframes for entity type {}; KltytonBedrockRuntime will retry on later client ticks.",
                            entity.getType(), exception);
                }
            }
        }
    }

    public static void clear() {
        WARNED_ENTITY_TYPES.clear();
        lastLevel = null;
        lastGameTime = Long.MIN_VALUE;
    }
}
