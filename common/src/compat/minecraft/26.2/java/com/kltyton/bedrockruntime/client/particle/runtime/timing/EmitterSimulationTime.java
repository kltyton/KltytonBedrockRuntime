package com.kltyton.bedrockruntime.client.particle.runtime.timing;

import com.kltyton.bedrockruntime.client.particle.runtime.ParticleEmitter;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;

/** Keeps frame scheduling and nanosecond emitter times alongside public tick counters. */
public final class EmitterSimulationTime {
    private static final int FRAMES_PER_UPDATE = 2;
    private final ParticleEmitter emitter;
    private int simulationFrame;
    private int lastSimulationFrame = ~FRAMES_PER_UPDATE;
    private long pendingFrameNanos;
    private long updateDeltaNanos;
    private long simulationSequence;
    private long updatedFrameIndex = Long.MIN_VALUE;
    private long inputFrameIndex = Long.MIN_VALUE;
    private int renderFrame;
    private int lastAppearanceFrame = ~FRAMES_PER_UPDATE;
    private long renderInputFrameIndex = Long.MIN_VALUE;
    private boolean appearanceUpdate;
    private boolean shortRenderLifetime;
    private long ageNanos;
    private long previousAgeNanos;
    private long loopAgeNanos;
    private long lifetimeNanos;
    private long activeNanos;
    private long loopNanos;
    private int ageTicks;
    private int loopAgeTicks;
    private int lifetimeTicks;
    private int activeTicks;
    private int loopTicks;

    public EmitterSimulationTime(ParticleEmitter emitter) {
        this.emitter = emitter;
    }

    public boolean beginFrame(long deltaNanos) {
        return beginFrame(deltaNanos, true);
    }

    public boolean beginFrame(long deltaNanos, boolean advanceEmitterAge) {
        long currentFrame = RenderFrameContext.frameIndex();
        if (inputFrameIndex == currentFrame) return updatedThisFrame();
        inputFrameIndex = currentFrame;
        updateDeltaNanos = 0L;
        pendingFrameNanos += deltaNanos;
        int frame = ++simulationFrame;
        if (frame - lastSimulationFrame < FRAMES_PER_UPDATE) return false;
        lastSimulationFrame = frame;
        updateDeltaNanos = pendingFrameNanos;
        pendingFrameNanos = 0L;
        updatedFrameIndex = RenderFrameContext.frameIndex();
        simulationSequence++;
        if (advanceEmitterAge) beginStep(updateDeltaNanos);
        return true;
    }

    public long updateDeltaNanos() {
        return updateDeltaNanos;
    }

    public long simulationSequence() {
        return simulationSequence;
    }

    public boolean updatedThisFrame() {
        return updatedFrameIndex == RenderFrameContext.frameIndex();
    }

    public float motionInterpolation() {
        return (float) (simulationFrame - lastSimulationFrame) / FRAMES_PER_UPDATE;
    }

    public void beginRenderFrame() {
        long frame = RenderFrameContext.frameIndex();
        if (renderInputFrameIndex == frame) return;
        renderInputFrameIndex = frame;
        int current = ++renderFrame;
        appearanceUpdate = current - lastAppearanceFrame >= FRAMES_PER_UPDATE;
        if (appearanceUpdate) lastAppearanceFrame = current;
        shortRenderLifetime = false;
    }

    public boolean appearanceUpdate() {
        return appearanceUpdate;
    }

    private boolean needsRenderWarmup() {
        return lastAppearanceFrame < 2 || lastSimulationFrame < 2;
    }

    public void noteRenderLifetime(long lifetime) {
        if (lifetime != 0L && lifetime < 150_000_001L) shortRenderLifetime = true;
    }

    public boolean forceWarmupAppearance() {
        return needsRenderWarmup() && shortRenderLifetime;
    }

    public boolean readyToRender() {
        return !needsRenderWarmup() || shortRenderLifetime;
    }

    public float appearanceInterpolation() {
        return (float) (renderFrame - lastAppearanceFrame) / FRAMES_PER_UPDATE;
    }

    public void beginStep(long deltaNanos) {
        synchronizeAge();
        synchronizeLoopAge();
        previousAgeNanos = ageNanos;
        ageNanos += deltaNanos;
        loopAgeNanos += deltaNanos;
    }

    public void finishStep() {
        synchronizeAge();
        synchronizeLoopAge();
        emitter.age++;
        ageTicks = emitter.age;
    }

    public float elapsedSeconds() {
        synchronizeAge();
        return ParticleSimulationClock.seconds(ageNanos);
    }

    public float ageSeconds() {
        synchronizeAge();
        return ParticleSimulationClock.seconds(ageNanos);
    }

    public long elapsedNanos() {
        synchronizeAge();
        return ageNanos;
    }

    public long previousElapsedNanos() {
        synchronizeAge();
        return previousAgeNanos;
    }

    public float lifetimeSeconds() {
        if (emitter.lifetime != lifetimeTicks) {
            lifetimeTicks = emitter.lifetime;
            lifetimeNanos = ParticleSimulationClock.nanos(lifetimeTicks * nominalStep());
        }
        return ParticleSimulationClock.seconds(lifetimeNanos);
    }

    public void setLifetimeSeconds(float seconds) {
        setLifetimeNanos(ParticleSimulationClock.nanos(seconds));
    }

    public boolean hasExceededLifetime() {
        synchronizeAge();
        lifetimeSeconds();
        return ageNanos >= lifetimeNanos;
    }

    public float activeSeconds() {
        if (emitter.activeTime != activeTicks) {
            activeTicks = emitter.activeTime;
            activeNanos = ParticleSimulationClock.nanos(activeTicks * nominalStep());
        }
        return ParticleSimulationClock.seconds(activeNanos);
    }

    public void setActiveSeconds(float seconds) {
        activeNanos = ParticleSimulationClock.nanos(seconds);
        emitter.activeTime = (int) (seconds / nominalStep());
        activeTicks = emitter.activeTime;
    }

    private float loopSeconds() {
        if (emitter.fullLoopTime != loopTicks) {
            loopTicks = emitter.fullLoopTime;
            loopNanos = ParticleSimulationClock.nanos(loopTicks * nominalStep());
        }
        return ParticleSimulationClock.seconds(loopNanos);
    }

    public float sleepSeconds() {
        activeSeconds();
        loopSeconds();
        return ParticleSimulationClock.seconds(loopNanos - activeNanos);
    }

    public void setLoopTimes(float active, float sleep) {
        setActiveSeconds(active);
        loopNanos = activeNanos + ParticleSimulationClock.nanos(sleep);
        emitter.fullLoopTime = (int) (ParticleSimulationClock.seconds(loopNanos) / nominalStep());
        loopTicks = emitter.fullLoopTime;
        setLifetimeNanos(loopNanos);
    }

    public boolean hasExceededLoopTime() {
        synchronizeLoopAge();
        loopSeconds();
        return loopAgeNanos >= loopNanos;
    }

    public boolean isInActiveLoopTime() {
        synchronizeLoopAge();
        activeSeconds();
        return loopAgeNanos <= activeNanos;
    }

    public void advanceLoopCounter() {
        emitter.loopingTime++;
        loopAgeTicks = emitter.loopingTime;
    }

    public void resetLoop() {
        emitter.age = 0;
        emitter.loopingTime = 0;
        ageTicks = 0;
        loopAgeTicks = 0;
        ageNanos = 0L;
        previousAgeNanos = 0L;
        loopAgeNanos = 0L;
    }

    private void setLifetimeNanos(long nanos) {
        lifetimeNanos = nanos;
        emitter.lifetime = (int) (ParticleSimulationClock.seconds(nanos) / nominalStep());
        lifetimeTicks = emitter.lifetime;
    }

    private void synchronizeAge() {
        if (emitter.age != ageTicks) {
            ageTicks = emitter.age;
            ageNanos = ParticleSimulationClock.nanos(ageTicks * nominalStep());
            previousAgeNanos = ageNanos;
        }
    }

    private void synchronizeLoopAge() {
        if (emitter.loopingTime != loopAgeTicks) {
            loopAgeTicks = emitter.loopingTime;
            loopAgeNanos = ParticleSimulationClock.nanos(loopAgeTicks * nominalStep());
        }
    }

    private float nominalStep() {
        return ParticleSimulationClock.nominalTickSeconds(emitter.level);
    }
}
