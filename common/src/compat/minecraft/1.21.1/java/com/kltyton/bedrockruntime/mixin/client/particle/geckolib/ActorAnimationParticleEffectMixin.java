package com.kltyton.bedrockruntime.mixin.client.particle.geckolib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.client.particle.access.IPSParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter", remap = false)
public abstract class ActorAnimationParticleEffectMixin {
    @Inject(method = "buildParticleFrameData", at = @At("RETURN"), remap = false)
    private static void kltyton_bedrock_runtime$retainParticleFields(JsonObject json, CallbackInfoReturnable<ParticleKeyframeData[]> cir) {
        JsonObject effects = json.has("particle_effects") ? json.getAsJsonObject("particle_effects") : null;
        if (effects == null) return;
        ParticleKeyframeData[] frames = cir.getReturnValue();
        int index = 0;
        for (var entry : effects.entrySet()) {
            JsonObject effect = entry.getValue().getAsJsonObject();
            JsonElement bind = effect.get("bind_to_actor");
            Boolean bindToActor = bind == null || bind.isJsonNull() ? null : bind.getAsBoolean();
            ParticleKeyframeData frame = frames[index++];
            IPSParticleKeyframeData.of(frame).kltyton_bedrock_runtime$setSource(frame.script(), bindToActor);
        }
    }
}
