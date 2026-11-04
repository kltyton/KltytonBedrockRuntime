package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.client.particle.api.IEmitterComponent;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.List;

/// This component specifies the frame of reference of the emitter.
///
/// Applies only when the emitter is attached to an entity.
///
/// When `position` is true, the particles will simulate in entity space, otherwise they will simulate in world space.
///
/// `rotation` works the same way for `rotation`.
///
/// Default is false for both, which makes the particles emit relative to the emitter, then simulate independently of the emitter.
///
/// Note that `rotation` = true and `position` = false is an invalid option.
///
/// `velocity` adds emitter velocity only when position and rotation are both false.
public record EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) implements IEmitterComponent {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("emitter_local_space");
    public static final Codec<EmitterLocalSpace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("position", false).forGetter(EmitterLocalSpace::position),
            Codec.BOOL.optionalFieldOf("rotation", false).forGetter(EmitterLocalSpace::rotation),
            Codec.BOOL.optionalFieldOf("velocity", false).forGetter(EmitterLocalSpace::velocity)
    ).apply(instance, EmitterLocalSpace::new));

    public EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) {
        velocity = velocity && !position && !rotation;
        if (rotation && !position) {
            com.kltyton.bedrockruntime.particle.BedrockParticles.LOGGER.warn("Local-space rotation requires local-space position; ignoring rotation");
            rotation = false;
        }
        this.position = position;
        this.rotation = rotation;
        this.velocity = velocity;
    }

    @Override
    public Codec<EmitterLocalSpace> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public int order() {
        return 800;
    }

    @Override
    public String toString() {
        return "EmitterLocalSpace[" +
                "position=" + position + ", " +
                "rotation=" + rotation + ", " +
                "velocity=" + velocity + ']';
    }
}
