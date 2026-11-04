/*
 * Derived from ThreeDimensionParticle 1.2.1 (upstream LGPL-3.0-only, westernat and contributors),
 * reference commits f9eab801e014a40c568cd4d37fde2aa9caa00fef and
 * 593269a462a7d060227fbb0dbd4c1651cb613e6c. Reimplemented for Kltyton Bedrock Runtime.
 */package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.CodecCompat;
import com.kltyton.bedrockruntime.client.particle.definition.DuplicateFieldDecoder;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleFrame;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleAnimationSelector;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleRuntimeState;
import com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleStateAccess;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Model-particle appearance component. Mirrors ThreeDimensionParticle 1.2.1's
 * {@code thr_dim_particle:appearance} fields while being registered under the KBR namespace by
 * {@code BedrockModelParticles}.
 *
 * <p>Frame selection happens in simulation ticks only; render passes never advance animation time
 * and never execute Molang events.</p>
 */
public record ParticleAppearanceModel(FloatMolangExp3 size,
                                      Optional<ParticleAppearanceBillboard.FaceCameraMode> faceCameraMode,
                                      ParticleAppearanceBillboard.Direction direction,
                                      ModelAnimation modelAnimation,
                                      Optional<String> model) implements IParticleComponent {
    public static final String ID_PATH = "model_appearance";
    public static final Codec<ModelAnimation> ANIMATION_CODEC = ModelAnimation.CODEC;
    public static final Codec<ParticleAppearanceModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecCompat.lenientOptionalFieldOf(FloatMolangExp3.ZERO_COMPONENT_CODEC, "size", FloatMolangExp3.ZERO)
                    .forGetter(ParticleAppearanceModel::size),
            DuplicateFieldDecoder.optionalFieldOf(ParticleAppearanceBillboard.FaceCameraMode.CODEC,
                            "face_camera_mode", "facing_camera_mode", "face_camera", "facing_camera")
                    .forGetter(ParticleAppearanceModel::faceCameraMode),
            CodecCompat.lenientOptionalFieldOf(ParticleAppearanceBillboard.Direction.CODEC, "direction",
                            ParticleAppearanceBillboard.Direction.DEFAULT)
                    .forGetter(ParticleAppearanceModel::direction),
            CodecCompat.lenientOptionalFieldOf(ANIMATION_CODEC, "model_animation", ModelAnimation.EMPTY)
                    .forGetter(ParticleAppearanceModel::modelAnimation),
            CodecCompat.lenientOptionalFieldOf(Codec.STRING, "model").forGetter(ParticleAppearanceModel::model)
    ).apply(instance, ParticleAppearanceModel::new));

    public ParticleAppearanceModel {
        size = size == null ? FloatMolangExp3.ZERO : size;
        faceCameraMode = faceCameraMode == null ? Optional.empty() : faceCameraMode;
        direction = direction == null ? ParticleAppearanceBillboard.Direction.DEFAULT : direction;
        modelAnimation = modelAnimation == null ? ModelAnimation.EMPTY : modelAnimation;
        model = model == null ? Optional.empty() : model;
    }

    @Override
    public Codec<ParticleAppearanceModel> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                size.exp1(), size.exp2(), size.exp3(),
                direction.customDirection().exp1(), direction.customDirection().exp2(),
                direction.customDirection().exp3());
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        updateState(instance, true);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        updateState(instance, true);
    }

    /**
     * Captures size/direction in the runtime's once-per-frame appearance evaluation.
     * Geometry submission consumes the captured values without evaluating expressions again.
     */
    public void renderAppearance(IMolangParticleInstance instance) {
        updateState(instance, false);
    }

    private void updateState(IMolangParticleInstance instance, boolean advanceFrame) {
        float[] calculated = size.calculate(instance);
        float width = sanitize(calculated.length > 0 ? calculated[0] : 0.0F);
        float height = sanitize(calculated.length > 1 ? calculated[1] : width);
        float depth = sanitize(calculated.length > 2 ? calculated[2] : Math.max(width, height));
        instance.setBillboardSize(new float[]{width, height, depth});

        updateFacing(instance);
        List<ModelParticleFrame> frames = modelAnimation.typeFrames();
        int frameCount = ModelParticleAnimationSelector.frameCount(frames, model.isPresent());
        instance.setMaxFrame(Math.max(1, frameCount));
        if (!advanceFrame) {
            int current = Math.max(0, Math.min(instance.getCurrentFrame(), Math.max(0, frameCount - 1)));
            instance.setCurrentFrame(current);
            publish(instance, frames, current);
            return;
        }
        int frame = ModelParticleAnimationSelector.frame(
                instance.tickAge(), instance.tickLifetime(), frameCount,
                modelAnimation.framesPerSecond(), modelAnimation.stretchToLifetime(),
                modelAnimation.loop());
        instance.setCurrentFrame(frame);
        publish(instance, frames, frame);
    }

    private void publish(IMolangParticleInstance instance, List<ModelParticleFrame> frames, int frame) {
        if (!(instance instanceof ModelParticleStateAccess access)) {
            return;
        }
        ModelParticleRuntimeState state = access.kltyton_bedrock_runtime$modelParticleState();
        String selected = ModelParticleAnimationSelector.modelAt(frames, model.orElse(""), frame,
                instance.getRandom4());
        if (selected == null || selected.isBlank()) {
            return;
        }
        try {
            state.select(ModelParticleModels.canonical(selected), null, false);
        } catch (IllegalArgumentException exception) {
            com.kltyton.bedrockruntime.client.particle.model.internal.ModelParticleDiagnostics
                    .invalidModelSelection(instance.getIdentity().toString(), selected);
            state.select("kltyton_bedrock_runtime:invalid_model", null, false);
        }
    }

    private void updateFacing(IMolangParticleInstance instance) {
        if (faceCameraMode.isPresent() && !faceCameraMode.get().isDirection()) {
            return;
        }
        if (direction.mode() == ParticleAppearanceBillboard.Direction.Mode.CUSTOM_DIRECTION) {
            float[] values = direction.customDirection().calculate(instance);
            instance.getFacingDirection().set(values[0], values[1], values[2]);
            if (instance.getFacingDirection().lengthSquared() > Mth.EPSILON) {
                instance.getFacingDirection().normalize();
            }
        } else {
            double speedSquared = Mth.lengthSquared(instance.getXd(), instance.getYd(), instance.getZd());
            double threshold = direction.minSpeedThreshold() * direction.minSpeedThreshold()
                    * instance.getVelocityTimeScale() * instance.getVelocityTimeScale();
            if (speedSquared >= threshold) {
                instance.getFacingDirection().set(instance.getXd(), instance.getYd(), instance.getZd()).normalize();
            }
        }
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0F;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public int order() {
        return 700;
    }

    @Override
    public String toString() {
        return "ParticleAppearanceModel{" +
                "size=" + size +
                ", faceCameraMode=" + faceCameraMode +
                ", direction=" + direction +
                ", modelAnimation=" + modelAnimation +
                ", model=" + model +
                '}';
    }

    /** Original {@code model_animation} block. */
    public record ModelAnimation(List<ModelParticleFrame> typeFrames, float framesPerSecond,
                                 boolean stretchToLifetime, boolean loop) {
        public static final ModelAnimation EMPTY = new ModelAnimation(List.of(), 1.0F, false, false);
        private static final Codec<ModelParticleFrame.Entry> ENTRY_CODEC =
                Codec.either(Codec.STRING, EntryValue.CODEC)
                        .xmap(either -> either.map(model -> new ModelParticleFrame.Entry(model, 1),
                                        EntryValue::toEntry),
                                entry -> entry.weight() == 1
                                        ? Either.left(entry.model())
                                        : Either.right(new EntryValue(entry.model(), entry.weight())));
        private static final Codec<ModelParticleFrame> FRAME_CODEC =
                Codec.either(ENTRY_CODEC.listOf(), ENTRY_CODEC)
                        .xmap(either -> either.map(ModelParticleFrame::new,
                                        entry -> new ModelParticleFrame(List.of(entry))),
                                frame -> frame.entries().size() == 1
                                        ? Either.right(frame.entries().get(0)) : Either.left(frame.entries()));
        private static final Codec<List<ModelParticleFrame>> FRAMES_CODEC =
                Codec.either(FRAME_CODEC, FRAME_CODEC.listOf())
                        .xmap(either -> either.map(List::of, Function.identity()),
                                list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
        public static final Codec<ModelAnimation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CodecCompat.lenientOptionalFieldOf(FRAMES_CODEC, "type_frames", List.of()).forGetter(ModelAnimation::typeFrames),
                CodecCompat.lenientOptionalFieldOf(Codec.FLOAT, "frames_per_second", 1.0F)
                        .forGetter(ModelAnimation::framesPerSecond),
                CodecCompat.lenientOptionalFieldOf(Codec.BOOL, "stretch_to_lifetime", false)
                        .forGetter(ModelAnimation::stretchToLifetime),
                CodecCompat.lenientOptionalFieldOf(Codec.BOOL, "loop", false).forGetter(ModelAnimation::loop)
        ).apply(instance, ModelAnimation::new));

        public ModelAnimation {
            typeFrames = List.copyOf(typeFrames == null ? List.of() : typeFrames);
            if (!Float.isFinite(framesPerSecond) || framesPerSecond <= 0.0F) {
                framesPerSecond = 1.0F;
            }
        }

        private record EntryValue(String model, int weight) {
            private static final Codec<EntryValue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    DuplicateFieldDecoder.fieldOf(Codec.STRING, "model_type", "model", "model_id")
                            .forGetter(EntryValue::model),
                    CodecCompat.lenientOptionalFieldOf(Codec.INT, "weight", 1).forGetter(EntryValue::weight)
            ).apply(instance, EntryValue::new));

            private ModelParticleFrame.Entry toEntry() {
                return new ModelParticleFrame.Entry(model, weight);
            }
        }
    }

    /** Returns the model appearance component of an effect, or {@code null}. */
    public static @Nullable ParticleAppearanceModel of(IMolangParticleInstance instance) {
        if (instance == null || instance.getPreset() == null || instance.getPreset().effect == null) {
            return null;
        }
        var component = instance.getPreset().effect.components.get(
                com.kltyton.bedrockruntime.particle.BedrockParticles.asResource(ID_PATH));
        return component instanceof ParticleAppearanceModel appearance ? appearance : null;
    }
}

