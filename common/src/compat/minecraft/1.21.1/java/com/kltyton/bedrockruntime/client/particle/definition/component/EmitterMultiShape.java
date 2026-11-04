package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.definition.ConditionDispatch;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.List;

public record EmitterMultiShape(List<ConditionDispatch<EmitterShape>> shapes) implements IEmitterComponent {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID, "emitter_multi_shape");
    public static final Codec<EmitterMultiShape> CODEC = ConditionDispatch.codec(EmitterShape.MAP, EmitterShape::dispatchId).listOf().xmap(EmitterMultiShape::new, EmitterMultiShape::shapes);

    @Override
    public Codec<EmitterMultiShape> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return shapes.stream().<MolangExp>map(ConditionDispatch::condition).toList();
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public void update(ParticleEmitter emitter) {
        if (emitter.spawned) return;
        boolean instant = emitter.getPreset().emitterRateType == EmitterRate.Type.INSTANT;
        boolean processed = false;
        for (ConditionDispatch<EmitterShape> shape : shapes) {
            if (shape.condition().get(emitter)) {
                shape.value().update(emitter);
                processed = true;
                if (instant) emitter.spawned = false;
            }
        }
        if (instant && processed) emitter.spawned = true;
    }
}
