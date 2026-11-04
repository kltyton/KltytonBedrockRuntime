package com.kltyton.bedrockruntime.mixin.entity;

import com.kltyton.bedrockruntime.api.state.runtime.ObbEntityStates;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CN: 在原版实体保存/加载入口附加 KltytonBedrockRuntime 数据，避免要求每个实体重复覆盖保存方法。
 * EN: Adds KltytonBedrockRuntime data at vanilla entity save/load entrypoints without requiring every entity to override persistence methods.
 */
@Mixin(Entity.class)
public abstract class EntityObbDataMixin implements com.kltyton.bedrockruntime.api.state.runtime.ObbEntityStateAccess {
    @org.spongepowered.asm.mixin.Unique
    private com.kltyton.bedrockruntime.api.state.runtime.ObbEntityState kltyton_bedrock_runtime$attachedState;

    @Override
    public com.kltyton.bedrockruntime.api.state.runtime.ObbEntityState kltyton_bedrock_runtime$getAttachedState() {
        return kltyton_bedrock_runtime$attachedState;
    }

    @Override
    public void kltyton_bedrock_runtime$setAttachedState(com.kltyton.bedrockruntime.api.state.runtime.ObbEntityState state) {
        kltyton_bedrock_runtime$attachedState = state;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$saveObbData(ValueOutput output, CallbackInfo callbackInfo) {
        ObbEntityStates.save((Entity) (Object) this, output);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void kltyton_bedrock_runtime$loadObbData(ValueInput input, CallbackInfo callbackInfo) {
        ObbEntityStates.load((Entity) (Object) this, input);
    }
}
