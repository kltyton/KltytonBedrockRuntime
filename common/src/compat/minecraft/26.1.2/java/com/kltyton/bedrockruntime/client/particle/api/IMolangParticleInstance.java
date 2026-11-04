package com.kltyton.bedrockruntime.client.particle.api;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticlePreset;
import com.kltyton.bedrockruntime.client.particle.runtime.ParticleFrameMotion;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleEngine;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;

import java.util.List;

public interface IMolangParticleInstance extends MolangInstance {
    default Particle self() {
        return (Particle) this;
    }

    int getAge();

    default float getElapsedTime() { return getAge() * getInvTickRate(); }

    default long getElapsedTimeNanos() {
        return com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock.nanos(getElapsedTime());
    }

    default void setLifetimeSeconds(float seconds) {
        self().setLifetime(Math.max((int) (seconds / getInvTickRate()), 0));
    }

    void setEmitter(ParticleEmitter emitter);

    ParticlePreset getPreset();

    @Nullable TextureAtlasSprite getSprite();

    Vector3f getAcceleration();

    Vector3f getFacingDirection();

    default Vector3f getInterpolatedFacingDirection(Vector3f destination, float partialTick) {
        return destination.set(getFacingDirection());
    }

    Vector3f getInitialSpeed();

    void setXRot(float x);

    void setYRot(float y);

    void setZRot(float z);

    void setZRotD(float delta);

    float getZRotD();

    void setCollisionDrag(float drag);

    void setCoefficientOfRestitution(float coefficient);

    void setExpireOnContact(boolean b);

    void setCollisionRadius(float radius);

    float getCollisionRadius();

    void setComponents(List<IParticleComponent> components);

    float getScaleU();

    float getScaleV();

    void setBillboardSize(float[] size);

    /** Returns a current billboard-size component when the implementation can expose it. */
    default @Nullable Float getBillboardSize(int axis) {
        return null;
    }

    /** Returns a current color component when the implementation can expose it. */
    default @Nullable Float getColorComponent(int component) {
        return null;
    }

    /** Returns the current spin in radians when the implementation can expose it. */
    default @Nullable Float getRotation() {
        return null;
    }

    /** Returns current u, v, width or height in the billboard's declared texture units. */
    default @Nullable Float getTextureCoordinate(int component) {
        return null;
    }

    void setUvSize(float[] size);

    /** Exposes the mutable flipbook base UV in declared texture units when supported. */
    default @Nullable float[] getUvBase() {
        return null;
    }

    float[] getUvSize();

    void setUvStep(float[] step);

    float[] getUvStep();

    void setMaxFrame(int frame);

    int getMaxFrame();

    void setCurrentFrame(int frame);

    int getCurrentFrame();

    void setInsideKillPlane(boolean b);

    boolean isInsideKillPlane();

    void setParticleGroup(ParticleLimit group);

    void setLastTimeline(int last);

    int getLastTimeline();

    /** Seconds represented by the host particle's velocity and rotation-rate fields. */
    default float getVelocityTimeScale() {
        return getInvTickRate();
    }

    double getXd();

    double getYd();

    double getZd();

    double getX();

    double getY();

    double getZ();

    double getPreviousX();

    double getPreviousY();

    double getPreviousZ();

    default Vector3f toSimulationSpace(double x, double y, double z, Vector3f destination) {
        return getEmitter().toParticleSimulationSpace(x, y, z, destination);
    }

    default Vector3f getWorldPosition(Vector3f dest, float partialTick) {
        dest.set((float) getX(), (float) getY(), (float) getZ());
        ParticleEmitter emitter = getEmitter();
        if (emitter != null) emitter.local2World(dest, partialTick);
        return dest;
    }

    default Vector3f getInterpolatedWorldPosition(Vector3f dest, float partialTick) {
        Particle particle = self();
        dest.set((float) net.minecraft.util.Mth.lerp((double) partialTick, getPreviousX(), getX()),
                (float) net.minecraft.util.Mth.lerp((double) partialTick, getPreviousY(), getY()),
                (float) net.minecraft.util.Mth.lerp((double) partialTick, getPreviousZ(), getZ()));
        ParticleEmitter emitter = getEmitter();
        if (emitter != null) emitter.local2World(dest, partialTick);
        return dest;
    }

    /** Returns the current parametric render sample, if this effect has one. */
    default @Nullable ParticleFrameMotion getFrameMotion() {
        var manager = MolangParticleEngine.managerFor(this);
        return manager == null ? null : manager.frameMotion(this);
    }

    /** Returns the sampled world position used for the current render pass. */
    default Vector3f getRenderWorldPosition(Vector3f dest, float partialTick) {
        ParticleFrameMotion motion = getFrameMotion();
        return motion != null && motion.position() != null
                ? dest.set(motion.position()) : getInterpolatedWorldPosition(dest, partialTick);
    }

    default @Nullable Quaternionf getLocalSpaceRotation() {
        return getEmitter().getLocalSpaceRotation();
    }

    void setPosO(double x, double y, double z);

    void setColor(float red, float green, float blue, float alpha);

    void setUV(float u, float v, float w, float h);

    void setCollision(boolean bool);

    void discard();

    boolean isDiscarded();

    // region default
    default void moveDirectly(double dx, double dy, double dz) {
        float radius = getCollisionRadius();
        double px = getX() + dx;
        double py = getY() + dy;
        double pz = getZ() + dz;
        self().setBoundingBox(new AABB(
                px - radius,
                py,
                pz - radius,
                px + radius,
                py + radius + radius,
                pz + radius
        ));
        ((com.kltyton.bedrockruntime.client.particle.access.ParticlePositionAccess) self())
                .kltyton_bedrock_runtime$syncPositionFromBoundingBox();
    }

    @Override
    default float tickAge() {
        return RenderFrameContext.ageInTicks(getAge()) * getInvTickRate();
    }

    @Override
    default float tickLifetime() {
        return self().getLifetime() * getInvTickRate();
    }

    @Override
    default Identifier getIdentity() {
        return getEmitter().particleId;
    }

    @Override
    default Vec3 getPosition() {
        return new Vec3(getX(), getY(), getZ());
    }

    @Override
    default @Nullable Entity getAttachedEntity() {
        return getEmitter().getAttachedEntity();
    }

    @Override
    default float getInvTickRate() {
        // Custom native particles retain Java tick velocity units.
        return com.kltyton.bedrockruntime.client.particle.runtime.timing.ParticleSimulationClock.nominalTickSeconds(getLevel());
    }
    // endregion
}
