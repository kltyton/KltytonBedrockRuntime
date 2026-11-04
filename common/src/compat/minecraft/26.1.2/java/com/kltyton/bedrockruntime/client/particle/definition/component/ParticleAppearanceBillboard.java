package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.DuplicateFieldDecoder;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp2;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp3;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.access.ITextureAtlasSprite;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record ParticleAppearanceBillboard(FloatMolangExp2 size, FaceCameraMode faceCameraMode, Direction direction, UV uv) implements IParticleComponent {
    public static final Identifier ID = Identifier.withDefaultNamespace("particle_appearance_billboard");
    private static final FloatMolangExp2 DEFAULT_SIZE = new FloatMolangExp2(FloatMolangExp.ONE, FloatMolangExp.ONE);
    private static final Codec<FloatMolangExp2> SIZE_CODEC = Codec.list(
            FloatMolangExp.CODEC.orElse((java.util.function.Consumer<String>) error ->
                    BedrockParticles.LOGGER.warn("Invalid billboard size component: {}", error), FloatMolangExp.ONE), 2, 2
    ).xmap(values -> new FloatMolangExp2(values.getFirst(), values.get(1)),
            value -> List.of(value.exp1(), value.exp2()));
    private static final Codec<FloatMolangExp2> ZERO_PAIR_CODEC = Codec.list(
            FloatMolangExp.CODEC.orElse((java.util.function.Consumer<String>) error ->
                    BedrockParticles.LOGGER.warn("Invalid billboard coordinate component: {}", error), FloatMolangExp.ZERO), 2, 2
    ).xmap(values -> new FloatMolangExp2(values.getFirst(), values.get(1)),
            value -> List.of(value.exp1(), value.exp2()));
    public static final Codec<ParticleAppearanceBillboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SIZE_CODEC.orElse((java.util.function.Consumer<String>) error ->
                            BedrockParticles.LOGGER.warn("Invalid billboard size: {}", error), DEFAULT_SIZE)
                    .optionalFieldOf("size", DEFAULT_SIZE).forGetter(ParticleAppearanceBillboard::size),
            DuplicateFieldDecoder.optionalFieldOfPreferred(FaceCameraMode.CODEC, "facing_camera_mode", "face_camera_mode")
                    .xmap(value -> value.orElse(FaceCameraMode.LOOKAT_XYZ), java.util.Optional::of)
                    .forGetter(ParticleAppearanceBillboard::faceCameraMode),
            Direction.CODEC.lenientOptionalFieldOf("direction", Direction.DEFAULT).forGetter(ParticleAppearanceBillboard::direction),
            UV.CODEC.lenientOptionalFieldOf("uv", UV.EMPTY).forGetter(ParticleAppearanceBillboard::uv)
    ).apply(instance, ParticleAppearanceBillboard::new));

    @Override
    public Codec<ParticleAppearanceBillboard> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                size.exp1(), size.exp2(),
                direction.customDirection.exp1(), direction.customDirection.exp2(), direction.customDirection.exp3(),
                uv.uv.exp1(), uv.uv.exp2(),
                uv.uvSize.exp1(), uv.uvSize.exp2(),
                uv.flipbook.baseUV.exp1(), uv.flipbook.baseUV.exp2(),
                uv.flipbook.sizeUV.exp1(), uv.flipbook.sizeUV.exp2(),
                uv.flipbook.stepUV.exp1(), uv.flipbook.stepUV.exp2(),
                uv.flipbook.maxFrame
        );
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        if (uv != UV.EMPTY && uv.flipbook != UV.Flipbook.EMPTY) updateFlipbookFrame(instance);
    }

    private float updateFlipbookFrame(IMolangParticleInstance instance) {
        UV.Flipbook flipbook = uv.flipbook;
        float maximum = flipbook.maxFrame.calculate(instance);
        instance.setMaxFrame((int) maximum);
        float lifetime = (float) instance.getVars().getValue("variable.particle_lifetime", instance);
        float age = (float) instance.getVars().getValue("variable.particle_age", instance);
        float rate = flipbook.stretchToLifetime ? 1.0F / (lifetime / maximum) : flipbook.framesPerSecond;
        float phase = rate * age;
        float frame = (float) Math.copySign(Math.floor(Math.abs((double) phase) + 0.5), phase);
        if (flipbook.loop) frame %= maximum;
        else if (maximum <= frame) frame = maximum - 1.0F;
        instance.setCurrentFrame((int) frame);
        return frame;
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        renderAppearance(instance);
    }

    private void doFacingCameraMode(IMolangParticleInstance instance) {
        if (direction.mode == Direction.Mode.CUSTOM_DIRECTION) {
            float[] values = direction.customDirection.calculate(instance);
            instance.getFacingDirection().set(values[0], values[1], values[2]);
        } else {
            var motion = instance.getFrameMotion();
            var sampledDirection = motion == null ? null : motion.direction();
            if (sampledDirection != null && sampledDirection.lengthSquared() > Mth.EPSILON) {
                instance.getFacingDirection().set(sampledDirection);
            } else if (Mth.lengthSquared(instance.getXd(), instance.getYd(), instance.getZd())
                    > instance.getPreset().minSpeedThresholdSqr * Mth.square(instance.getVelocityTimeScale())) {
                instance.getFacingDirection().set(instance.getXd(), instance.getYd(), instance.getZd()).normalize();
            }
        }
    }

    private void doSize(IMolangParticleInstance instance) {
        float width = size.exp1().calculate(instance);
        float height = size.exp2().calculate(instance);
        instance.setBillboardSize(new float[]{width > 0.0F ? width : 0.0F, height > 0.0F ? height : 0.0F});
    }

    /** Evaluates billboard appearance for the current render frame. */
    public void renderAppearance(IMolangParticleInstance instance) {
        doSize(instance);
        if (uv != UV.EMPTY) {
            if (uv.flipbook == UV.Flipbook.EMPTY) {
                updateSimpleUV(instance);
            } else {
                float frame = updateFlipbookFrame(instance);
                instance.setUvSize(uv.flipbook.getSizeUV(instance));
                instance.getUvSize()[0] *= instance.getScaleU();
                instance.getUvSize()[1] *= instance.getScaleV();
                instance.setUvStep(uv.flipbook.getStepUV(instance));
                instance.getUvStep()[0] *= instance.getScaleU();
                instance.getUvStep()[1] *= instance.getScaleV();
                updateFlipbookUV(instance, frame);
            }
        }
        doFacingCameraMode(instance);
    }

    @Override
    public boolean requireUpdate() {
        return false;
    }

    @Override
    public int order() {
        return 700;
    }

    private void updateSimpleUV(IMolangParticleInstance instance) {
        TextureAtlasSprite sprite = instance.getSprite();
        if (sprite == null) return;
        float[] base = uv.uv.calculate(instance);
        float[] size = uv.uvSize.calculate(instance);
        float x = instance.getPreset().effect.usesTerrainAtlas() ? 0
                : sprite.getU0() * ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginX();
        float y = instance.getPreset().effect.usesTerrainAtlas() ? 0
                : sprite.getV0() * ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginY();
        instance.setUV(x + base[0] * instance.getScaleU(), y + base[1] * instance.getScaleV(), size[0] * instance.getScaleU(), size[1] * instance.getScaleV());
    }



    private void updateFlipbookUV(IMolangParticleInstance instance, float frame) {
        TextureAtlasSprite sprite = instance.getSprite();
        if (sprite == null) return;
        float[] previousBase = instance.getUvBase();
        float baseV = uv.flipbook.baseUV.exp2().calculate(instance);
        float baseU = uv.flipbook.baseUV.exp1().calculate(instance);
        float[] base = new float[]{baseU, baseV};
        if (previousBase != null) {
            previousBase[0] = base[0];
            previousBase[1] = base[1];
        }
        float u = instance.getUvStep()[0] * frame;
        float v = instance.getUvStep()[1] * frame;
        float x = instance.getPreset().effect.usesTerrainAtlas() ? 0
                : sprite.getU0() * ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginX();
        float y = instance.getPreset().effect.usesTerrainAtlas() ? 0
                : sprite.getV0() * ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginY();
        instance.setUV(x + base[0] * instance.getScaleU() + u, y + base[1] * instance.getScaleV() + v, instance.getUvSize()[0], instance.getUvSize()[1]);
    }

    @Override
    public String toString() {
        return "ParticleAppearanceBillboard{" +
                "size=" + size +
                ", faceCameraMode=" + faceCameraMode +
                ", direction=" + direction +
                ", uv=" + uv +
                '}';
    }

    public enum FaceCameraMode implements StringRepresentable {
        DO_NOTHING,
        ROTATE_XYZ,
        ROTATE_Y,
        LOOKAT_XYZ,
        LOOKAT_Y,
        DIRECTION_X,
        DIRECTION_Y,
        DIRECTION_Z,
        LOOKAT_DIRECTION,
        EMITTER_TRANSFORM_XY,
        EMITTER_TRANSFORM_XZ,
        EMITTER_TRANSFORM_YZ;

        public static final Codec<FaceCameraMode> CODEC = StringRepresentable.fromEnum(FaceCameraMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean isDirection() {
            return this == LOOKAT_DIRECTION || this == DIRECTION_X || this == DIRECTION_Y || this == DIRECTION_Z;
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    public record Direction(Mode mode, float minSpeedThreshold, FloatMolangExp3 customDirection) {
        public static final Direction DEFAULT = new Direction(Direction.Mode.DERIVE_FROM_VELOCITY, 0.1F, FloatMolangExp3.ZERO);
        public static final MapCodec<Direction> CUSTOM_MODE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("custom_direction", FloatMolangExp3.ZERO).forGetter(Direction::customDirection)
        ).apply(instance, l -> new Direction(Mode.CUSTOM_DIRECTION, 0, l)));
        public static final MapCodec<Direction> SPEED_MODE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.lenientOptionalFieldOf("min_speed_threshold", 0.0F).forGetter(Direction::minSpeedThreshold)
        ).apply(instance, f -> new Direction(Mode.DERIVE_FROM_VELOCITY, f, FloatMolangExp3.ZERO)));
        public static final Codec<Direction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Mode.CODEC.lenientOptionalFieldOf("mode").forGetter(direction -> Optional.of(direction.mode)),
                Codec.FLOAT.lenientOptionalFieldOf("min_speed_threshold").forGetter(direction ->
                        direction.mode == Mode.DERIVE_FROM_VELOCITY ? Optional.of(direction.minSpeedThreshold) : Optional.empty()),
                FloatMolangExp3.ZERO_COMPONENT_CODEC.lenientOptionalFieldOf("custom_direction").forGetter(direction ->
                        direction.mode == Mode.CUSTOM_DIRECTION ? Optional.of(direction.customDirection) : Optional.empty())
        ).apply(instance, (mode, threshold, custom) -> {
            Mode resolved = mode.orElse(Mode.DERIVE_FROM_VELOCITY);
            return resolved == Mode.CUSTOM_DIRECTION
                    ? new Direction(resolved, 0.0F, custom.orElse(FloatMolangExp3.ZERO))
                    : new Direction(resolved, threshold.orElse(0.0F), FloatMolangExp3.ZERO);
        }));

        @Override
        public String toString() {
            return "Direction{" +
                    "mode=" + mode +
                    ", minSpeedThreshold=" + minSpeedThreshold +
                    ", customDirection=" + customDirection +
                    '}';
        }

        public enum Mode implements StringRepresentable {
            CUSTOM_DIRECTION,
            DERIVE_FROM_VELOCITY;

            public static final Codec<Mode> CODEC = new StringRepresentable.EnumCodec<>(values(), name -> {
                if ("custom_direction".equals(name) || "custom".equals(name)) return CUSTOM_DIRECTION;
                return DERIVE_FROM_VELOCITY;
            });

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }

            @Override
            public String toString() {
                return getSerializedName();
            }
        }
    }

    /// Specifies the UVs for the particle.
    ///
    /// @param texturewidth
    /// @param textureheight Specifies the assumed texture width/height, defaults to 1<p>
    ///                      When set to 1, UV's work just like normalized UV's<p>
    ///                      When set to the texture width/height, this works like texels
    /// @param uv
    /// @param uvSize        Assuming the specified texture width and height, use these uv coordinates.<p>
    ///                      Evaluated every frame
    public record UV(int texturewidth, int textureheight, FloatMolangExp2 uv, FloatMolangExp2 uvSize, Flipbook flipbook) {
        public static final UV EMPTY = new UV(1, 1, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, Flipbook.EMPTY);
        public static final Codec<UV> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DuplicateFieldDecoder.fieldOf(ExtraCodecs.POSITIVE_INT, "texturewidth", "texture_width").orElse(1).forGetter(UV::texturewidth),
                DuplicateFieldDecoder.fieldOf(ExtraCodecs.POSITIVE_INT, "textureheight", "texture_height").orElse(1).forGetter(UV::textureheight),
                ZERO_PAIR_CODEC.lenientOptionalFieldOf("uv", FloatMolangExp2.ZERO).forGetter(UV::uv),
                SIZE_CODEC.lenientOptionalFieldOf("uv_size", DEFAULT_SIZE).forGetter(UV::uvSize),
                Flipbook.CODEC.lenientOptionalFieldOf("flipbook", Flipbook.EMPTY).forGetter(UV::flipbook)
        ).apply(instance, UV::new));

        @Override
        public String toString() {
            return "UV{" +
                    "texturewidth=" + texturewidth +
                    ", textureheight=" + textureheight +
                    ", uv=" + uv +
                    ", uvSize=" + uvSize +
                    ", flipbook=" + flipbook +
                    '}';
        }

        /// Alternate way via specifying a flipbook animation
        ///
        /// A flipbook animation uses pieces of the texture to animate, by stepping over time from one `frame` to another
        ///
        /// @param baseUV            Upper-left corner of starting UV patch
        /// @param sizeUV            Size of UV patch
        /// @param stepUV            How far to move the UV patch each frame
        /// @param framesPerSecond   Default frames per second
        /// @param maxFrame          Maximum frame number, with first frame being frame 1
        /// @param stretchToLifetime Optional, adjust fps to match lifetime of particle. Default=false
        /// @param loop              Optional, makes the animation loop when it reaches the end? Default=false
        public record Flipbook(FloatMolangExp2 baseUV, FloatMolangExp2 sizeUV, FloatMolangExp2 stepUV, float framesPerSecond, FloatMolangExp maxFrame, boolean stretchToLifetime, boolean loop) {
            public static final Flipbook EMPTY = new Flipbook(FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, 1, FloatMolangExp.ZERO, false, false);
            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ZERO_PAIR_CODEC.lenientOptionalFieldOf("base_UV", FloatMolangExp2.ZERO).forGetter(Flipbook::baseUV),
                    SIZE_CODEC.lenientOptionalFieldOf("size_UV", DEFAULT_SIZE).forGetter(Flipbook::sizeUV),
                    ZERO_PAIR_CODEC.lenientOptionalFieldOf("step_UV", FloatMolangExp2.ZERO).forGetter(Flipbook::stepUV),
                    Codec.FLOAT.lenientOptionalFieldOf("frames_per_second", 0.0F).forGetter(Flipbook::framesPerSecond),
                    FloatMolangExp.CODEC.lenientOptionalFieldOf("max_frame", FloatMolangExp.ONE).forGetter(Flipbook::maxFrame),
                    Codec.BOOL.lenientOptionalFieldOf("stretch_to_lifetime", false).forGetter(Flipbook::stretchToLifetime),
                    Codec.BOOL.lenientOptionalFieldOf("loop", false).forGetter(Flipbook::loop)
            ).apply(instance, Flipbook::new));

            public float[] getSizeUV(IMolangParticleInstance instance) {
                return sizeUV.calculate(instance);
            }

            public float[] getStepUV(IMolangParticleInstance instance) {
                return stepUV.calculate(instance);
            }

            @Override
            public String toString() {
                return "Flipbook{" +
                        "baseUV=" + baseUV +
                        ", sizeUV=" + sizeUV +
                        ", stepUV=" + stepUV +
                        ", framesPerSecond=" + framesPerSecond +
                        ", maxFrame=" + maxFrame +
                        ", stretchToLifetime=" + stretchToLifetime +
                        ", loop=" + loop +
                        '}';
            }
        }
    }
}