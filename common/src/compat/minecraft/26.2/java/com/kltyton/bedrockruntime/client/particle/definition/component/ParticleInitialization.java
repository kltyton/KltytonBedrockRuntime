package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;

import java.util.List;

/// Starts the particle with a specified render expression.
public record ParticleInitialization(FloatMolangExp perRenderExpression, FloatMolangExp perUpdateExpression) implements IParticleComponent {
    public static final Identifier ID = Identifier.withDefaultNamespace("particle_initialization");
    public static final Codec<ParticleInitialization> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.lenientOptionalFieldOf("per_render_expression", FloatMolangExp.ZERO).forGetter(ParticleInitialization::perRenderExpression),
            FloatMolangExp.CODEC.lenientOptionalFieldOf("per_update_expression", FloatMolangExp.ZERO).forGetter(ParticleInitialization::perUpdateExpression)
    ).apply(instance, ParticleInitialization::new));

    @Override
    public Codec<ParticleInitialization> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(perRenderExpression, perUpdateExpression);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        perUpdateExpression.calculate(instance);
    }

    public static void render(IMolangParticleInstance instance) {
        instance.getPreset().prepareVariables(instance);
        ParticleInitialization initialization = instance.getPreset().initialization;
        if (initialization != null) initialization.perRenderExpression.calculate(instance);
        instance.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        var components = instance.getPreset().effect.components;
        if (components.get(ParticleAppearanceBillboard.ID) instanceof ParticleAppearanceBillboard billboard) {
            billboard.renderAppearance(instance);
        }
        var modelAppearance = ParticleAppearanceModel.of(instance);
        if (modelAppearance != null) {
            modelAppearance.renderAppearance(instance);
        }
        if (components.get(ParticleAppearanceTinting.ID) instanceof ParticleAppearanceTinting tinting) {
            tinting.apply(instance);
        }
        if (components.get(ParticleAppearanceCameraDistanceFade.ID) instanceof ParticleAppearanceCameraDistanceFade
                || ParticleAppearanceCameraDistanceFade.appliesTo(instance.getIdentity().toString())) {
            ParticleAppearanceCameraDistanceFade.INSTANCE.apply(instance);
        }
    }

    /// invoked through[EmitterShape#emittingParticle]
    @Override
    public void apply(IMolangParticleInstance instance) {}

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public int order() {
        return 800;
    }

    @Override
    public String toString() {
        return "ParticleInitialization{" +
                "perRenderExpression=" + perRenderExpression +
                ", perUpdateExpression=" + perUpdateExpression +
                '}';
    }
}
