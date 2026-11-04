package com.kltyton.bedrockruntime.client.particle.runtime;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;
import com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleMotionCollision;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleLifeTimeEvents;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleLifetimeExpression;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleLifetimeKillPlane;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleExpireIfInBlocks;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleExpireIfNotInBlocks;
import com.kltyton.bedrockruntime.client.particle.runtime.physics.ParticleCollision;
import com.kltyton.bedrockruntime.client.particle.definition.event.EventResolver;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.access.ITextureAtlasSprite;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MolangParticleInstance extends TextureSheetParticle implements IMolangParticleInstance {
    private long particleAgeNanos;
    private long previousParticleAgeNanos;
    private long particleLifetimeNanos;
    private float particleStepSeconds;
    private final float velocityTimeScale;
    private Vec3 simulationOrigin = Vec3.ZERO;
    private int lifetimeTicks;
    private boolean tickLifetime;
    private boolean evaluatingUpdate;
    private boolean previousMotionSample;
    private long lastSimulationSequence;
    private boolean appearancePrepared;
    private final org.joml.Matrix3f renderBasis = new org.joml.Matrix3f();
    private final Vector3f renderBasisScratch = new Vector3f();
    private boolean renderBasisActive;
    private final float[] previousBillboardSize = new float[2];
    private final float[] previousColor = new float[4];
    private final Vector3f previousFacingDirection = new Vector3f();
    public static final int FULL_LIGHT = 0xF000F0;

    protected final ParticlePreset preset;
    protected ParticleVariableTable vars;
    protected float originX;
    protected float originY;
    private long atlasRevision;
    private ResourceLocation atlasSpriteName;
    private boolean definitionSprite = true;

    protected Vector3f acceleration = new Vector3f();
    protected Vector3f facingDirection = new Vector3f();
    protected Vector3f initialSpeed = new Vector3f();
    protected final Vector3f renderPosition = new Vector3f();
    protected float xRot = 0.0F;
    protected float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    protected float rolld = 0.0F;
    protected boolean hasCollision = false;
    protected float collisionDrag = 0.0F;
    protected float coefficientOfRestitution = 0.0F;
    protected float collisionRadius = 0.0F;
    protected boolean expireOnContact = false;

    protected final double particleRandom1;
    protected final double particleRandom2;
    protected final double particleRandom3;
    protected final double particleRandom4;
    protected List<IParticleComponent> components = List.of();
    protected ParticleEmitter emitter;

    protected float scaleU;
    protected float scaleV;
    protected float[] billboardSize = new float[2];
    protected float[] uvSize;
    protected final float[] uvBase = new float[2];
    protected float[] uvStep;
    protected int maxFrame = 1;
    protected int currentFrame = 0;
    protected float[] UV;

    protected boolean insideKillPlane;
    private boolean expirationEventFired;
    protected ParticleGroup particleGroup;
    protected int lastTimeline = 0;

    public MolangParticleInstance(ParticlePreset preset, ClientLevel level, double x, double y, double z, RandomSource random) {
        super(level, x, y, z);
        this.friction = 1.0F;
        this.preset = preset;
        setSprite(preset.effect.description.parameters().getTexture(preset.effect.usesTerrainAtlas()));
        this.originX = ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginX();
        this.originY = ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginY();
        this.scaleU = (preset.effect.usesTerrainAtlas() ? originX : sprite.contents().width()) * preset.invTextureWidth;
        this.scaleV = (preset.effect.usesTerrainAtlas() ? originY : sprite.contents().height()) * preset.invTextureHeight;
        this.atlasRevision = com.kltyton.bedrockruntime.client.particle.atlas.ParticleAtlasRevision.current();
        this.atlasSpriteName = sprite.contents().name();
        if (preset.effect.usesTerrainAtlas()) this.UV = new float[]{0, 0, 1, 1};

        this.particleRandom1 = random.nextFloat();
        this.particleRandom2 = random.nextFloat();
        this.particleRandom3 = random.nextFloat();
        this.particleRandom4 = random.nextFloat();
        this.particleStepSeconds = ParticleSimulationClock.nominalTickSeconds(level);
        this.velocityTimeScale = particleStepSeconds;
        this.lifetimeTicks = (int) (1.0F / particleStepSeconds);
        this.lifetime = lifetimeTicks;
        this.particleLifetimeNanos = 1_000_000_000L;
    }

    public void includeInCurrentUpdate() {
        lastSimulationSequence = emitter.getSimulationSequence() - 1L;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public float getElapsedTime() {
        return ParticleSimulationClock.seconds(particleAgeNanos);
    }

    @Override
    public long getElapsedTimeNanos() {
        return particleAgeNanos;
    }

    @Override
    public float tickAge() {
        float current = ParticleSimulationClock.seconds(evaluatingUpdate ? previousParticleAgeNanos : particleAgeNanos);
        float duration = tickLifetime();
        return duration < current ? duration : Math.max(current, 0.0F);
    }

    @Override
    public float tickLifetime() {
        return tickLifetime ? lifetimeTicks * ParticleSimulationClock.nominalTickSeconds(level)
                : ParticleSimulationClock.seconds(particleLifetimeNanos);
    }

    @Override
    public float getInvTickRate() {
        return particleStepSeconds;
    }

    @Override
    public float getVelocityTimeScale() {
        return velocityTimeScale;
    }

    @Override
    public void setLifetimeSeconds(float seconds) {
        particleLifetimeNanos = ParticleSimulationClock.nanos(seconds);
        lifetimeTicks = (int) (seconds / ParticleSimulationClock.nominalTickSeconds(level));
        tickLifetime = false;
        super.setLifetime(lifetimeTicks);
    }

    @Override
    public void setLifetime(int lifetime) {
        lifetimeTicks = lifetime;
        tickLifetime = true;
        super.setLifetime(lifetime);
    }

    @Override
    public int getLifetime() {
        return lifetimeTicks;
    }

    @Override
    public void setEmitter(ParticleEmitter emitter) {
        this.emitter = emitter;
        this.simulationOrigin = emitter.getPosition();
        this.vars = new ParticleVariableTable(preset.vars, emitter.vars);
        this.particleStepSeconds = emitter.invTickRate;
        this.lastSimulationSequence = emitter.getSimulationSequence();
    }

    @Override
    public ParticlePreset getPreset() {
        return preset;
    }

    @Override
    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    @Override
    public Vector3f getAcceleration() {
        return acceleration;
    }

    @Override
    public Vector3f getFacingDirection() {
        return facingDirection;
    }

    @Override
    public Vector3f getInterpolatedFacingDirection(Vector3f destination, float partialTick) {
        float fraction = previousMotionSample && emitter != null ? emitter.getMotionInterpolation() : 1.0F;
        return destination.set(previousFacingDirection).lerp(facingDirection, fraction);
    }

    @Override
    public Vector3f getInitialSpeed() {
        return initialSpeed;
    }

    @Override
    public void setXRot(float x) {
        this.xRot = x;
    }

    @Override
    public void setYRot(float y) {
        this.yRot = y;
    }

    @Override
    public void setZRot(float z) {
        this.roll = z;
    }

    @Override
    public @Nullable Float getRotation() {
        ensureAppearanceForRender();
        return roll;
    }

    @Override
    public @Nullable Float getColorComponent(int component) {
        ensureAppearanceForRender();
        return switch (component) {
            case 0 -> rCol;
            case 1 -> gCol;
            case 2 -> bCol;
            case 3 -> alpha;
            default -> null;
        };
    }

    @Override
    public void setZRotD(float delta) {
        this.rolld = delta;
    }

    @Override
    public float getZRotD() {
        return rolld;
    }

    @Override
    public void setCollisionDrag(float drag) {
        this.collisionDrag = drag;
    }

    @Override
    public void setCoefficientOfRestitution(float coefficient) {
        this.coefficientOfRestitution = coefficient;
    }

    @Override
    public void setExpireOnContact(boolean b) {
        this.expireOnContact = b;
    }

    @Override
    public void setCollisionRadius(float radius) {
        this.collisionRadius = radius;
    }

    @Override
    public float getCollisionRadius() {
        return collisionRadius;
    }

    @Override
    public void setComponents(List<IParticleComponent> components) {
        this.components = components;
    }

    @Override
    public float getScaleU() {
        return scaleU;
    }

    @Override
    public float getScaleV() {
        return scaleV;
    }

    @Override
    public void setBillboardSize(float[] size) {
        this.billboardSize = size;
    }

    private void ensureAppearanceForRender() {
        if (evaluatingUpdate) return;
        MolangParticleManager manager = MolangParticleEngine.managerFor(this);
        if (manager != null) manager.evaluatePerRenderExpressions(RenderFrameContext.framePartialTick());
    }

    @Override
    public @Nullable Float getBillboardSize(int axis) {
        ensureAppearanceForRender();
        return billboardSize == null || axis < 0 || axis >= billboardSize.length ? null : billboardSize[axis];
    }

    @Override
    public void setUvSize(float[] size) {
        this.uvSize = size;
    }

    @Override
    public float[] getUvBase() {
        return uvBase;
    }

    @Override
    public float[] getUvSize() {
        return uvSize;
    }

    @Override
    public void setUvStep(float[] step) {
        this.uvStep = step;
    }

    @Override
    public float[] getUvStep() {
        return uvStep;
    }

    @Override
    public void setMaxFrame(int frame) {
        this.maxFrame = Math.max(frame, 1);
    }

    @Override
    public int getMaxFrame() {
        return maxFrame;
    }

    @Override
    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    @Override
    public int getCurrentFrame() {
        return currentFrame;
    }

    @Override
    public void setInsideKillPlane(boolean b) {
        this.insideKillPlane = b;
    }

    @Override
    public boolean isInsideKillPlane() {
        return insideKillPlane;
    }

    @Override
    public void setParticleGroup(ParticleGroup group) {
        this.particleGroup = group;
    }

    @Override
    public void setLastTimeline(int last) {
        this.lastTimeline = last;
    }

    @Override
    public int getLastTimeline() {
        return lastTimeline;
    }

    @Override
    public double getXd() {
        return xd;
    }

    @Override
    public double getYd() {
        return yd;
    }

    @Override
    public double getZd() {
        return zd;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public double getPreviousX() { return xo; }

    @Override
    public double getPreviousY() { return yo; }

    @Override
    public double getPreviousZ() { return zo; }

    @Override
    public void setPosO(double x, double y, double z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public void setColor(float red, float green, float blue, float alpha) {
        super.setColor(red, green, blue);
        super.setAlpha(alpha);
    }

    @Override
    public void setUV(float u, float v, float w, float h) {
        if (UV == null) this.UV = new float[4];
        this.UV[0] = u / originX;
        this.UV[1] = v / originY;
        this.UV[2] = (u + w) / originX;
        this.UV[3] = (v + h) / originY;
    }

    @Override
    public @Nullable Float getTextureCoordinate(int component) {
        return switch (component) {
            case 0 -> (getU0() - (preset.effect.usesTerrainAtlas() ? 0 : sprite.getU0())) * originX / scaleU;
            case 1 -> (getV0() - (preset.effect.usesTerrainAtlas() ? 0 : sprite.getV0())) * originY / scaleV;
            case 2 -> (getU1() - getU0()) * originX / scaleU;
            case 3 -> (getV1() - getV0()) * originY / scaleV;
            default -> null;
        };
    }

    @Override
    public void setCollision(boolean bool) {
        this.hasCollision = bool;
    }

    @Override
    public void discard() {
        remove();
    }

    @Override
    public boolean isDiscarded() {
        return removed;
    }

    @Override
    public VariableTable getVars() {
        return vars;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public double getRandom1() {
        return particleRandom1;
    }

    @Override
    public double getRandom2() {
        return particleRandom2;
    }

    @Override
    public double getRandom3() {
        return particleRandom3;
    }

    @Override
    public double getRandom4() {
        return particleRandom4;
    }

    @Override
    public ParticleEmitter getEmitter() {
        return emitter;
    }

    private boolean storesOriginOffset() {
        return emitter != null && !emitter.isParticleSimulationWorldSpace() && !emitter.hasLocalParticlePosition();
    }

    private void synchronizeSimulationOrigin() {
        if (!storesOriginOffset()) return;
        Vec3 origin = emitter.getPosition();
        if (simulationOrigin.equals(origin)) return;
        double dx = origin.x - simulationOrigin.x;
        double dy = origin.y - simulationOrigin.y;
        double dz = origin.z - simulationOrigin.z;
        x += dx;
        y += dy;
        z += dz;
        xo += dx;
        yo += dy;
        zo += dz;
        setBoundingBox(getBoundingBox().move(dx, dy, dz));
        simulationOrigin = origin;
    }

    @Override
    public void setPos(double x, double y, double z) {
        synchronizeSimulationOrigin();
        super.setPos(x, y, z);
    }

    @Override
    public Vector3f toSimulationSpace(double x, double y, double z, Vector3f destination) {
        return storesOriginOffset()
                ? destination.set((float) (x - simulationOrigin.x), (float) (y - simulationOrigin.y),
                        (float) (z - simulationOrigin.z))
                : destination.set((float) x, (float) y, (float) z);
    }

    @Override
    public Vector3f getWorldPosition(Vector3f dest, float partialTick) {
        toSimulationSpace(x, y, z, dest);
        if (emitter != null) emitter.simulationToWorld(dest, partialTick);
        return dest;
    }

    @Override
    public Vector3f getInterpolatedWorldPosition(Vector3f dest, float partialTick) {
        partialTick = previousMotionSample && emitter != null ? emitter.getMotionInterpolation() : 1.0F;
        toSimulationSpace(Mth.lerp((double) partialTick, xo, x),
                Mth.lerp((double) partialTick, yo, y),
                Mth.lerp((double) partialTick, zo, z), dest);
        if (emitter != null) emitter.simulationToWorld(dest, 1.0F);
        return dest;
    }

    @Override
    public Vector3f getRenderWorldPosition(Vector3f dest, float partialTick) {
        ParticleFrameMotion motion = getFrameMotion();
        return motion != null && motion.position() != null
                ? dest.set(motion.position()) : getInterpolatedWorldPosition(dest, partialTick);
    }

    @Override
    protected float getU0() {
        return UV == null ? super.getU0() : UV[0];
    }

    @Override
    protected float getV0() {
        return UV == null ? super.getV0() : UV[1];
    }

    @Override
    protected float getU1() {
        return UV == null ? super.getU1() : UV[2];
    }

    @Override
    protected float getV1() {
        return UV == null ? super.getV1() : UV[3];
    }

    @Override
    public void tick() {
        advanceSimulation();
    }

    public void advanceSimulation() {
        if (!isAlive()) return;
        MolangParticleManager manager = MolangParticleEngine.managerFor(this);
        if (manager == null || !manager.isAdvancingFrame() || emitter == null || !emitter.updatedThisFrame()) return;
        long sequence = emitter.getSimulationSequence();
        if (lastSimulationSequence == sequence) return;
        lastSimulationSequence = sequence;
        synchronizeSimulationOrigin();
        long deltaNanos = emitter.getUpdateDeltaNanos();
        float deltaSeconds = ParticleSimulationClock.seconds(deltaNanos);
        this.particleStepSeconds = deltaSeconds;
        this.previousParticleAgeNanos = particleAgeNanos;
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xRotO = xRot;
        this.yRotO = yRot;
        this.oRoll = roll;
        this.previousFacingDirection.set(facingDirection);
        evaluatingUpdate = true;
        try {
            preset.prepareVariables(this);
            for (IParticleComponent component : components) {
                if (isLifetimeComponent(component) || component instanceof ParticleMotionCollision) continue;
                component.update(this);
                if (!isAlive()) return;
            }
            for (IParticleComponent component : components) {
                if (!(component instanceof ParticleMotionCollision)) continue;
                component.update(this);
                if (hasPhysics && hasCollision) {
                    resolveMotionCollision(xo, yo, zo, !previousMotionSample);
                }
                if (!isAlive()) return;
            }
            long previousAge = particleAgeNanos;
            int previousTickAge = age;
            particleAgeNanos += deltaNanos;
            age++;
            for (IParticleComponent component : components) {
                if (component instanceof ParticleLifeTimeEvents events) events.update(this, previousAge, particleAgeNanos);
                if (!isAlive()) return;
            }
            if (tickLifetime ? previousTickAge >= lifetimeTicks : previousAge >= particleLifetimeNanos) {
                remove();
                return;
            }
            for (IParticleComponent component : components) {
                if (isLifetimeComponent(component) && !(component instanceof ParticleLifeTimeEvents)) component.update(this);
                if (!isAlive()) return;
            }
            if (!previousMotionSample) {
                setPosO(x, y, z);
                xRotO = xRot;
                yRotO = yRot;
                oRoll = roll;
                previousFacingDirection.set(facingDirection);
            }
            previousMotionSample = true;
        } finally {
            evaluatingUpdate = false;
        }
    }

    private static boolean isLifetimeComponent(IParticleComponent component) {
        return component instanceof ParticleLifeTimeEvents || component instanceof ParticleLifetimeExpression
                || component instanceof ParticleLifetimeKillPlane || component instanceof ParticleExpireIfInBlocks
                || component instanceof ParticleExpireIfNotInBlocks;
    }

    boolean hasAppearanceData() {
        return appearancePrepared;
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        atlasSpriteName = sprite.contents().name();
        definitionSprite = false;
        replaceSprite(sprite);
    }

    void refreshAtlasSprite() {
        long revision = com.kltyton.bedrockruntime.client.particle.atlas.ParticleAtlasRevision.current();
        if (atlasRevision == revision) return;
        atlasRevision = revision;
        boolean wholeAtlas = preset.effect.usesTerrainAtlas();
        TextureAtlas atlas = wholeAtlas ? DescriptionMaterial.TERRAIN_SHEET.getAtlas()
                : preset.effect.description.parameters().material().getAtlas();
        if (!atlas.location().equals(sprite.atlasLocation())) return;
        ResourceLocation resolvedName = definitionSprite && atlas.location().equals(TextureAtlas.LOCATION_PARTICLES)
                ? com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource.currentSprite(
                        atlasSpriteName, preset.effect.originalTextureForExport())
                : atlasSpriteName;
        TextureAtlasSprite updated;
        if (definitionSprite && !wholeAtlas && atlasSpriteName.equals(MissingTextureAtlasSprite.getLocation())) {
            ResourceLocation recovered = atlas.location().equals(TextureAtlas.LOCATION_PARTICLES)
                    ? com.kltyton.bedrockruntime.client.particle.bedrock.BedrockSpriteSource.missingSprite(
                            preset.effect.description.identifier(), preset.effect.description.parameters().texture())
                    : null;
            updated = recovered == null ? preset.effect.description.parameters().getTexture() : atlas.getSprite(recovered);
        } else {
            updated = atlas.getSprite(resolvedName);
        }
        if (definitionSprite && !updated.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            atlasSpriteName = updated.contents().name();
        }
        replaceSprite(updated);
    }

    private void replaceSprite(TextureAtlasSprite updated) {
        if (updated == sprite) return;

        boolean wholeAtlas = preset.effect.usesTerrainAtlas();
        float oldScaleU = scaleU;
        float oldScaleV = scaleV;
        float u = 0, v = 0, width = 0, height = 0;
        if (UV != null) {
            u = (UV[0] - (wholeAtlas ? 0 : sprite.getU0())) * originX / oldScaleU;
            v = (UV[1] - (wholeAtlas ? 0 : sprite.getV0())) * originY / oldScaleV;
            width = (UV[2] - UV[0]) * originX / oldScaleU;
            height = (UV[3] - UV[1]) * originY / oldScaleV;
        }
        super.setSprite(updated);
        originX = ((ITextureAtlasSprite) updated).kltyton_bedrock_runtime$getOriginX();
        originY = ((ITextureAtlasSprite) updated).kltyton_bedrock_runtime$getOriginY();
        scaleU = (wholeAtlas ? originX : updated.contents().width()) * preset.invTextureWidth;
        scaleV = (wholeAtlas ? originY : updated.contents().height()) * preset.invTextureHeight;
        if (UV != null) {
            float x = wholeAtlas ? 0 : updated.getU0() * originX;
            float y = wholeAtlas ? 0 : updated.getV0() * originY;
            setUV(x + u * scaleU, y + v * scaleV, width * scaleU, height * scaleV);
        }
        if (uvSize != null) {
            uvSize[0] *= scaleU / oldScaleU;
            uvSize[1] *= scaleV / oldScaleV;
        }
        if (uvStep != null) {
            uvStep[0] *= scaleU / oldScaleU;
            uvStep[1] *= scaleV / oldScaleV;
        }
    }

    void beginAppearanceUpdate() {
        previousBillboardSize[0] = billboardSize == null || billboardSize.length < 2 ? super.getQuadSize(1.0F) : billboardSize[0];
        previousBillboardSize[1] = billboardSize == null || billboardSize.length < 2 ? super.getQuadSize(1.0F) : billboardSize[1];
        previousColor[0] = rCol;
        previousColor[1] = gCol;
        previousColor[2] = bCol;
        previousColor[3] = alpha;
    }

    void finishAppearanceUpdate() {
        if (!appearancePrepared) {
            beginAppearanceUpdate();
            appearancePrepared = true;
        }
    }

    long renderLifetimeNanos() {
        return tickLifetime ? ParticleSimulationClock.nanos(tickLifetime()) : particleLifetimeNanos;
    }

    private float getRenderColorComponent(int component) {
        float current = component == 0 ? rCol : component == 1 ? gCol : component == 2 ? bCol : alpha;
        return appearancePrepared && emitter != null
                ? Mth.lerp(emitter.getAppearanceInterpolation(), previousColor[component], current) : current;
    }

    public boolean hasPreviousMotionSample() {
        return previousMotionSample;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        ensureAppearanceForRender();
        float width = getBillboardWidth(partialTicks);
        float height = getBillboardHeight(partialTicks);
        return Math.max(width, height);
    }

    public int getLightCoords(float partialTick) {
        ensureAppearanceForRender();
        return getLightColor(partialTick);
    }

    public boolean kltyton_bedrock_runtime$isVisible(@NotNull Frustum frustum, @NotNull Camera camera, float partialTick) {
        return frustum.isVisible(renderBounds(camera, partialTick));
    }

    private AABB renderBounds(Camera camera, float partialTick) {
        partialTick = previousMotionSample && emitter != null ? emitter.getMotionInterpolation() : 1.0F;
        float width = getBillboardWidth(partialTick);
        float height = getBillboardHeight(partialTick);
        double extentX;
        double extentY;
        double extentZ;
        if (camera != null) {
            prepareRenderBasis(camera, partialTick);
            extentX = Math.abs(renderBasis.m00() * width) + Math.abs(renderBasis.m10() * height);
            extentY = Math.abs(renderBasis.m01() * width) + Math.abs(renderBasis.m11() * height);
            extentZ = Math.abs(renderBasis.m02() * width) + Math.abs(renderBasis.m12() * height);
        } else {
            extentX = extentY = extentZ = Math.max(width, height);
        }
        getRenderWorldPosition(renderPosition, partialTick);
        double centerX = renderPosition.x;
        double centerY = renderPosition.y;
        double centerZ = renderPosition.z;
        return new AABB(centerX - extentX, centerY - extentY, centerZ - extentZ,
                centerX + extentX, centerY + extentY, centerZ + extentZ);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        ensureAppearanceForRender();
        refreshAtlasSprite();
        if (!isAlive() || !appearancePrepared || emitter != null && !emitter.readyToRender()) return;
        partialTicks = previousMotionSample && emitter != null ? emitter.getMotionInterpolation() : 1.0F;
        Quaternionf quaternion = new Quaternionf();
        prepareRenderBasis(camera, partialTicks);
        renderBasis.getUnnormalizedRotation(quaternion);
        Vec3 camPos = camera.getPosition();
        getRenderWorldPosition(renderPosition, partialTicks);
        renderPosition.sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        renderBasisActive = true;
        try {
            renderRotatedQuad(buffer, quaternion, renderPosition.x, renderPosition.y, renderPosition.z, partialTicks);
        } finally {
            renderBasisActive = false;
        }
    }

    private void prepareRenderBasis(Camera camera, float partialTicks) {
        getFacingCameraMode().setRenderBasis(this, renderBasis, renderBasisScratch, camera, partialTicks);
        float renderX = Mth.lerp(partialTicks, xRotO, xRot);
        float renderY = Mth.lerp(partialTicks, yRotO, yRot);
        if (renderX != 0.0F) renderBasis.rotateX(renderX);
        if (renderY != 0.0F) renderBasis.rotateY(renderY);
        ParticleFrameMotion motion = getFrameMotion();
        float renderRoll = motion != null && motion.hasRotation() ? motion.rotation() : Mth.lerp(partialTicks, oRoll, roll);
        if (Math.abs(renderRoll / Mth.DEG_TO_RAD) > Math.ulp(1.0F)) renderBasis.rotateZ(renderRoll);
    }

    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
        float f1 = getU0();
        float f2 = getU1();
        float f3 = getV0();
        float f4 = getV1();
        int i = getLightColor(partialTicks);
        emitQuadVertex(buffer, quaternion, x, y, z, 1.0F, -1.0F, 0.0F, f2, f4, i);
        emitQuadVertex(buffer, quaternion, x, y, z, 1.0F, 1.0F, 0.0F, f2, f3, i);
        emitQuadVertex(buffer, quaternion, x, y, z, -1.0F, 1.0F, 0.0F, f1, f3, i);
        emitQuadVertex(buffer, quaternion, x, y, z, -1.0F, -1.0F, 0.0F, f1, f4, i);
    }

    protected void emitQuadVertex(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight) {
        renderPosition.set(xOffset * getBillboardWidth(1.0F), yOffset * getBillboardHeight(1.0F), 0.0F);
        if (renderBasisActive) renderPosition.mul(renderBasis);
        else renderPosition.rotate(quaternion);
        renderPosition.add(x, y, z);
        buffer.addVertex(renderPosition.x(), renderPosition.y(), renderPosition.z()).setUv(u, v)
                .setColor(getRenderColorComponent(0), getRenderColorComponent(1), getRenderColorComponent(2), getRenderColorComponent(3))
                .setLight(packedLight);
    }

    private float getBillboardWidth(float partialTick) {
        if (billboardSize == null || billboardSize.length < 2) return super.getQuadSize(partialTick);
        float width = appearancePrepared && emitter != null
                ? Mth.lerp(emitter.getAppearanceInterpolation(), previousBillboardSize[0], billboardSize[0]) : billboardSize[0];
        return Math.max(0.0F, width);
    }

    private float getBillboardHeight(float partialTick) {
        if (billboardSize == null || billboardSize.length < 2) return super.getQuadSize(partialTick);
        float height = appearancePrepared && emitter != null
                ? Mth.lerp(emitter.getAppearanceInterpolation(), previousBillboardSize[1], billboardSize[1]) : billboardSize[1];
        return Math.max(0.0F, height);
    }

    @Override
    public void move(double x, double y, double z) {
        synchronizeSimulationOrigin();
        double previousX = this.x;
        double previousY = this.y;
        double previousZ = this.z;
        moveDirectly(x, y, z);
        if (hasPhysics && hasCollision) resolveMotionCollision(previousX, previousY, previousZ, false);
    }

    public void moveSimulation(float dx, float dy, float dz) {
        synchronizeSimulationOrigin();
        boolean offset = storesOriginOffset();
        double ox = offset ? simulationOrigin.x : 0.0;
        double oy = offset ? simulationOrigin.y : 0.0;
        double oz = offset ? simulationOrigin.z : 0.0;
        float px = (float) (x - ox) + dx;
        float py = (float) (y - oy) + dy;
        float pz = (float) (z - oz) + dz;
        moveDirectly(ox + px - x, oy + py - y, oz + pz - z);
    }

    @Override
    public void moveDirectly(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        setBoundingBox(new AABB(x - collisionRadius, y - collisionRadius, z - collisionRadius,
                x + collisionRadius, y + collisionRadius, z + collisionRadius));
    }

    private void resolveMotionCollision(double previousX, double previousY, double previousZ, boolean firstSample) {
        Vector3f previous = new Vector3f((float) previousX, (float) previousY, (float) previousZ);
        Vector3f position = new Vector3f((float) x, (float) y, (float) z);
        Vec3 origin = Vec3.ZERO;
        if (emitter != null) {
            toSimulationSpace(previousX, previousY, previousZ, previous);
            toSimulationSpace(x, y, z, position);
            if (!emitter.isParticleSimulationWorldSpace()) origin = emitter.getPosition();
        }
        if (firstSample) previous.zero();
        Vec3 previousWorld = new Vec3(previous.x + (float) origin.x, previous.y + (float) origin.y,
                previous.z + (float) origin.z);
        Vec3 positionWorld = new Vec3(position.x + (float) origin.x, position.y + (float) origin.y,
                position.z + (float) origin.z);
        Vec3 velocity = new Vec3(xd / velocityTimeScale, yd / velocityTimeScale, zd / velocityTimeScale);
        float contactDrag = collisionDrag / velocityTimeScale * particleStepSeconds;
        ParticleCollision.Result result = ParticleCollision.resolve(level, previousWorld, positionWorld, velocity,
                collisionRadius, contactDrag, coefficientOfRestitution);
        Vector3f corrected = new Vector3f((float) result.position().x - (float) origin.x,
                (float) result.position().y - (float) origin.y, (float) result.position().z - (float) origin.z);
        Vec3 nextPosition = emitter == null ? result.position() : emitter.fromParticleSimulationSpace(corrected);
        Vec3 nextVelocity = result.velocity();
        onGround = result.collided() && result.normal().y != 0.0 && velocity.y < 0.0;
        if (result.collided() && particleAgeNanos > 0L) {
            if (!preset.collisionEvents.isEmpty()) {
                float impactSpeed = Math.abs((float) velocity.x * (float) result.normal().x
                        + (float) velocity.y * (float) result.normal().y
                        + (float) velocity.z * (float) result.normal().z);
                Vector3f eventVelocity = EventResolver.captureVelocity(this);
                org.joml.Matrix3f eventBasis = EventResolver.collisionBasis(eventVelocity, result.normal());
                for (ParticleMotionCollision.Event event : preset.collisionEvents) {
                    if (!(event.minSpeed() <= impactSpeed)) continue;
                    EventResolver.executeEvent(EventResolver.resolve(preset.effect.events, event.event()),
                            this, result.position(), eventVelocity, eventBasis);
                }
            }
            if (expireOnContact) {
                particleAgeNanos = tickLifetime ? ParticleSimulationClock.nanos(tickLifetime()) : particleLifetimeNanos;
                age = lifetimeTicks;
            }
        }
        moveDirectly(nextPosition.x - x, nextPosition.y - y, nextPosition.z - z);
        setParticleSpeed(nextVelocity.x * velocityTimeScale, nextVelocity.y * velocityTimeScale,
                nextVelocity.z * velocityTimeScale);
    }

    void discardForInvalidFrame() {
        expirationEventFired = true;
        remove();
    }

    @Override
    public void remove() {
        if (!removed && !expirationEventFired) {
            expirationEventFired = true;
            try {
                if (preset.lifeTimeEvents != null) preset.lifeTimeEvents.onExpiration(this);
            } finally {
                super.remove();
            }
        } else {
            super.remove();
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return preset.customRenderGroup != null ? preset.customRenderGroup
                : preset.renderType == null ? ParticleRenderType.NO_RENDER : preset.renderType;
    }

    @Override
    public @NotNull FaceCameraMode getFacingCameraMode() {
        return preset.facingCameraMode;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return preset.environmentLighting ? super.getLightColor(partialTick) : FULL_LIGHT;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.ofNullable(particleGroup);
    }

    @Override
    public ResourceLocation getIdentity() {
        return preset.effect.description.identifier();
    }

    @Override
    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public String diagnosticSummary() {
        return "identity=" + getIdentity() +
                ",pos=(" + x + "," + y + "," + z + ")" +
                ",speed=(" + xd + "," + yd + "," + zd + ")" +
                ",age=" + age +
                ",lifetime=" + getLifetime() +
                ",quadSize=" + getQuadSize(0.0F) +
                ",billboardSize=" + Arrays.toString(billboardSize) +
                ",color=(" + rCol + "," + gCol + "," + bCol + "," + alpha + ")" +
                ",spriteSize=" + sprite.contents().width() + "x" + sprite.contents().height() +
                ",atlasSize=" + originX + "x" + originY +
                ",scale=(" + scaleU + "," + scaleV + ")" +
                ",uv=" + Arrays.toString(UV) +
                ",uvSize=" + Arrays.toString(uvSize) +
                ",uvStep=" + Arrays.toString(uvStep) +
                ",frame=" + currentFrame + "/" + maxFrame +
                ",group=" + (particleGroup == null ? "none" : particleGroup.getLimit()) +
                ",motionDynamic=" + preset.motionDynamic +
                ",components=" + components.stream().map(component -> component.getClass().getSimpleName()).toList() +
                ",flags=" + diagnosticFlags();
    }

    private String diagnosticFlags() {
        StringBuilder builder = new StringBuilder();
        appendDiagnosticFlag(builder, getLifetime() <= 0, "lifetime<=0");
        appendDiagnosticFlag(builder, billboardSize == null || billboardSize.length < 2 || billboardSize[0] <= 0.0F || billboardSize[1] <= 0.0F, "size<=0");
        appendDiagnosticFlag(builder, alpha <= 0.0F, "alpha<=0");
        appendDiagnosticFlag(builder, getRenderType() == ParticleRenderType.NO_RENDER, "no_render_type");
        appendDiagnosticFlag(builder, sprite.contents().width() <= 0 || sprite.contents().height() <= 0, "sprite_empty");
        return builder.isEmpty() ? "none" : builder.toString();
    }

    private static void appendDiagnosticFlag(StringBuilder builder, boolean condition, String name) {
        if (!condition) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('|');
        }
        builder.append(name);
    }

    public static class Provider implements ParticleProvider<MolangParticleOption> {
        @Override
        public @Nullable Particle createParticle(@NotNull MolangParticleOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MolangParticleInstance(option.getPreset(), level, x, y, z, level.getRandom());
        }
    }
}
