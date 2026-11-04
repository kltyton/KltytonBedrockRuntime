package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.kltyton.bedrockruntime.client.particle.api.AddDefaultVariableEvent;
import com.kltyton.bedrockruntime.client.particle.molang.ItemClockFrameState;
import com.kltyton.bedrockruntime.client.particle.molang.ItemCompassFrameState;
import com.kltyton.bedrockruntime.client.particle.molang.ItemLodestoneFrames;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.access.IEntity;
import com.kltyton.bedrockruntime.client.particle.access.IMolangEntityState;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.Hashtable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity, IMolangEntityState {
    @Unique
    private VariableTable kltyton_bedrock_runtime$variableTable;
    @Unique private ItemClockFrameState kltyton_bedrock_runtime$itemClockFrame;
    @Unique private ItemCompassFrameState kltyton_bedrock_runtime$itemCompassFrame;
    @Unique private ItemCompassFrameState kltyton_bedrock_runtime$itemRecoveryCompassFrame;
    @Unique private int kltyton_bedrock_runtime$entityState;
    @Unique private boolean kltyton_bedrock_runtime$entityStateKnown;
    @Unique private @Nullable GlobalPos kltyton_bedrock_runtime$recoveryTarget;
    @Unique private float kltyton_bedrock_runtime$animationLifetime;
    @Unique private long kltyton_bedrock_runtime$animationLifetimeFrame = -1;
    @Unique private float kltyton_bedrock_runtime$animationTime;
    @Unique private float kltyton_bedrock_runtime$animationDeltaTime;
    @Unique private float kltyton_bedrock_runtime$animationFrameAlpha;
    @Unique private float kltyton_bedrock_runtime$keyframeLerpTime;

    @Override
    public float kltyton_bedrock_runtime$getKeyframeLerpTime() {
        return kltyton_bedrock_runtime$keyframeLerpTime;
    }

    @Override
    public void kltyton_bedrock_runtime$setKeyframeLerpTime(float ratio) {
        kltyton_bedrock_runtime$keyframeLerpTime = ratio;
    }

    @Override
    public float kltyton_bedrock_runtime$getAnimationDeltaTime() {
        return kltyton_bedrock_runtime$animationDeltaTime;
    }

    @Override
    public float kltyton_bedrock_runtime$getAnimationFrameAlpha() {
        return kltyton_bedrock_runtime$animationFrameAlpha;
    }

    @Override
    public float kltyton_bedrock_runtime$getAnimationTime() {
        return kltyton_bedrock_runtime$animationTime;
    }

    @Override
    public void kltyton_bedrock_runtime$setAnimationTime(float seconds) {
        kltyton_bedrock_runtime$animationTime = seconds;
    }

    @Override
    public float kltyton_bedrock_runtime$getAnimationLifetime() {
        return kltyton_bedrock_runtime$animationLifetime;
    }

    @Override
    public boolean kltyton_bedrock_runtime$hasAnimationContext() {
        return kltyton_bedrock_runtime$animationLifetimeFrame != -1;
    }

    @Override
    public void kltyton_bedrock_runtime$sampleAnimationLifetime(float partialTick) {
        long frame = RenderFrameContext.frameIndex();
        if (kltyton_bedrock_runtime$animationLifetimeFrame == frame) return;
        Entity entity = (Entity) (Object) this;
        float time = (entity.tickCount + partialTick) / 20.0F;
        kltyton_bedrock_runtime$animationDeltaTime = Math.max(0.0F, time - kltyton_bedrock_runtime$animationLifetime);
        kltyton_bedrock_runtime$animationLifetime += kltyton_bedrock_runtime$animationDeltaTime;
        kltyton_bedrock_runtime$animationFrameAlpha = partialTick;
        kltyton_bedrock_runtime$animationLifetimeFrame = frame;
    }

    @Override
    public boolean kltyton_bedrock_runtime$targetStateKnown() {
        return kltyton_bedrock_runtime$entityStateKnown && (kltyton_bedrock_runtime$entityState & MolangEntityStatePacketS2C.TARGET_KNOWN) != 0;
    }

    @Override
    public boolean kltyton_bedrock_runtime$hasTarget() {
        return (kltyton_bedrock_runtime$entityState & MolangEntityStatePacketS2C.HAS_TARGET) != 0;
    }

    @Override
    public boolean kltyton_bedrock_runtime$loveStateKnown() {
        return kltyton_bedrock_runtime$entityStateKnown && (kltyton_bedrock_runtime$entityState & MolangEntityStatePacketS2C.LOVE_KNOWN) != 0;
    }

    @Override
    public boolean kltyton_bedrock_runtime$isInLove() {
        return (kltyton_bedrock_runtime$entityState & MolangEntityStatePacketS2C.IN_LOVE) != 0;
    }

    @Override
    public boolean kltyton_bedrock_runtime$recoveryStateKnown() {
        return kltyton_bedrock_runtime$entityStateKnown && (kltyton_bedrock_runtime$entityState & MolangEntityStatePacketS2C.RECOVERY_KNOWN) != 0;
    }

    @Override
    public @Nullable GlobalPos kltyton_bedrock_runtime$recoveryTarget() {
        return kltyton_bedrock_runtime$recoveryTarget;
    }

    @Override
    public void kltyton_bedrock_runtime$setMolangEntityState(int flags, @Nullable GlobalPos recoveryTarget) {
        kltyton_bedrock_runtime$entityState = flags;
        kltyton_bedrock_runtime$entityStateKnown = true;
        kltyton_bedrock_runtime$recoveryTarget = recoveryTarget;
    }

    @Override
    public int kltyton_bedrock_runtime$getClockAnimationFrame() {
        if (kltyton_bedrock_runtime$itemClockFrame == null) kltyton_bedrock_runtime$itemClockFrame = new ItemClockFrameState();
        return kltyton_bedrock_runtime$itemClockFrame.frame((Entity) (Object) this);
    }

    @Override
    public int kltyton_bedrock_runtime$getCompassAnimationFrame() {
        if (kltyton_bedrock_runtime$itemCompassFrame == null) kltyton_bedrock_runtime$itemCompassFrame = new ItemCompassFrameState();
        return kltyton_bedrock_runtime$itemCompassFrame.frame((Entity) (Object) this, false);
    }

    @Override
    public int kltyton_bedrock_runtime$getRecoveryCompassAnimationFrame() {
        if (kltyton_bedrock_runtime$itemRecoveryCompassFrame == null) kltyton_bedrock_runtime$itemRecoveryCompassFrame = new ItemCompassFrameState();
        return kltyton_bedrock_runtime$itemRecoveryCompassFrame.frame((Entity) (Object) this, true);
    }

    @Override
    public int kltyton_bedrock_runtime$getLodestoneCompassAnimationFrame(ItemStack item) {
        return ItemLodestoneFrames.frame((Entity) (Object) this, item);
    }

    @Override
    public VariableTable kltyton_bedrock_runtime$getVariableTable() {
        if (kltyton_bedrock_runtime$variableTable == null) {
            Hashtable<String, Variable> table = new Hashtable<>();
            table.put("variable.entity_scale", new Variable("variable.entity_scale", p -> {
                if (p.getAttachedEntity() instanceof LivingEntity living) {
                    return living.getAttributeValue(Attributes.SCALE);
                }
                return 1.0;
            }));
            this.kltyton_bedrock_runtime$variableTable = new VariableTable(table, null);
            AddDefaultVariableEvent.EVENT.invoker().onAddDefaultVariables(new AddDefaultVariableEvent.Entity(kltyton_bedrock_runtime$variableTable, (Entity) (Object) this));
        }
        return kltyton_bedrock_runtime$variableTable;
    }
}
