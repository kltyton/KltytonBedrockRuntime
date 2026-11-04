package com.kltyton.bedrockruntime.client.particle.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.client.particle.api.ParticleEmitterAttachable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;

public interface IEntity extends IPSEntity, IEntityFireAnimation {
    VariableTable kltyton_bedrock_runtime$getVariableTable();
    float kltyton_bedrock_runtime$getAnimationLifetime();
    void kltyton_bedrock_runtime$sampleAnimationLifetime(float partialTick);
    float kltyton_bedrock_runtime$getAnimationTime();
    void kltyton_bedrock_runtime$setAnimationTime(float seconds);
    float kltyton_bedrock_runtime$getAnimationDeltaTime();
    float kltyton_bedrock_runtime$getAnimationFrameAlpha();
    float kltyton_bedrock_runtime$getKeyframeLerpTime();
    void kltyton_bedrock_runtime$setKeyframeLerpTime(float ratio);
    boolean kltyton_bedrock_runtime$hasAnimationContext();
    int kltyton_bedrock_runtime$getClockAnimationFrame();
    int kltyton_bedrock_runtime$getCompassAnimationFrame();
    int kltyton_bedrock_runtime$getRecoveryCompassAnimationFrame();
    int kltyton_bedrock_runtime$getLodestoneCompassAnimationFrame(ItemStack item);

    @Override
    default Vec3 getPos() {
        return ((Entity) this).position();
    }

    @Override
    default Level getLevel() {
        return ((Entity) this).level();
    }

    @Override
    default boolean isDiscarded() {
        return ((Entity) this).isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return kltyton_bedrock_runtime$getVariableTable();
    }

    static IEntity of(Entity entity) {
        return (IEntity) entity;
    }
}
