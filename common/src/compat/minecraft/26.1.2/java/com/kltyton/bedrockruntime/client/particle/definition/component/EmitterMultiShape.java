package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.definition.ConditionDispatch;
import com.kltyton.bedrockruntime.client.particle.definition.molang.BoolMolangExp;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;

import java.util.List;

public record EmitterMultiShape(List<ConditionDispatch<EmitterShape>> shapes) implements IEmitterComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(com.kltyton.bedrockruntime.particle.BedrockParticles.MODID, "emitter_multi_shape");
    public static final Codec<EmitterMultiShape> CODEC = ConditionDispatch.codec(EmitterShape.MAP, EmitterShape::dispatchId).listOf().xmap(EmitterMultiShape::new, EmitterMultiShape::shapes);

    @Override
    public Codec<EmitterMultiShape> codec() {
        return CODEC;
    }

    @Override
    public List<BoolMolangExp> getAllMolangExp() {
        return shapes.stream().map(ConditionDispatch::condition).toList();
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public void update(ParticleEmitter emitter) {
        if (emitter.spawned && !emitter.getPreset().hasSteadyRate && !emitter.isManual) return;
        boolean instant = emitter.getPreset().hasInstantRate && !emitter.getPreset().hasSteadyRate;
        boolean emitted = emitter.spawned;
        for (ConditionDispatch<EmitterShape> shape : shapes) {
            if (shape.condition().get(emitter)) {
                shape.value().update(emitter);
                if (instant) {
                    emitted |= emitter.spawned;
                    emitter.spawned = false;
                }
            }
        }
        if (instant) emitter.spawned = emitted;
    }
}
