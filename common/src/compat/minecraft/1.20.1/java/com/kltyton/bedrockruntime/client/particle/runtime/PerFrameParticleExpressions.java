package com.kltyton.bedrockruntime.client.particle.runtime;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleInitialization;
import com.kltyton.bedrockruntime.client.particle.definition.molang.RenderFrameContext;

import java.util.ArrayList;

/** Maintains the admitted particles whose render expressions run each frame. */
final class PerFrameParticleExpressions {
    private final MolangParticleManager owner;
    private final ArrayList<IMolangParticleInstance> particles = new ArrayList<>();
    private final Reference2IntOpenHashMap<IMolangParticleInstance> positions = new Reference2IntOpenHashMap<>();
    private int evaluationDepth;
    private boolean needsCompaction;
    private long renderPass;
    private long lastEvaluatedFrame = Long.MIN_VALUE;

    PerFrameParticleExpressions(MolangParticleManager owner) {
        this.owner = owner;
        positions.defaultReturnValue(-1);
    }

    void add(IMolangParticleInstance particle) {
        if (positions.containsKey(particle)) throw new IllegalStateException("Particle was already admitted");
        positions.put(particle, particles.size());
        particles.add(particle);
    }

    void remove(IMolangParticleInstance particle) {
        int index = positions.removeInt(particle);
        if (index < 0) throw new IllegalStateException("Particle was not admitted");
        if (evaluationDepth > 0) {
            particles.set(index, null);
            needsCompaction = true;
            return;
        }
        int last = particles.size() - 1;
        IMolangParticleInstance replacement = particles.remove(last);
        if (index != last) {
            particles.set(index, replacement);
            positions.put(replacement, index);
        }
    }

    void evaluate(float partialTick) {
        long frame = RenderFrameContext.frameIndex();
        if (lastEvaluatedFrame == frame) return;
        lastEvaluatedFrame = frame;
        owner.advanceFrame();
        evaluationDepth++;
        RenderFrameContext.enter(partialTick);
        int end = particles.size();
        long pass = ++renderPass;
        try {
            for (int index = 0; index < end; index++) {
                IMolangParticleInstance particle = particles.get(index);
                if (particle == null) continue;
                ParticleEmitter emitter = particle.getEmitter();
                emitter.prepareRenderVariables(this, pass);
                if (particle instanceof MolangParticleInstance builtIn) {
                    if (emitter.hasInvalidLocalFrame()) continue;
                    if (emitter.needsAppearanceUpdate() || !builtIn.hasAppearanceData()) {
                        builtIn.beginAppearanceUpdate();
                        ParticleInitialization.render(particle);
                        builtIn.finishAppearanceUpdate();
                    }
                    emitter.noteRenderLifetime(builtIn.renderLifetimeNanos());
                } else {
                    ParticleInitialization.render(particle);
                }
            }
            for (int index = 0; index < end; index++) {
                IMolangParticleInstance particle = particles.get(index);
                if (particle == null) continue;
                if (particle instanceof MolangParticleInstance builtIn
                        && particle.getEmitter().forceWarmupAppearance()) {
                    builtIn.beginAppearanceUpdate();
                    ParticleInitialization.render(particle);
                    builtIn.finishAppearanceUpdate();
                }
                owner.prepareFrameMotion(particle, partialTick);
            }
        } finally {
            RenderFrameContext.exit();
            evaluationDepth--;
            if (evaluationDepth == 0 && needsCompaction) compact();
        }
    }

    void advanceRetainedParticles(long deltaNanos) {
        evaluationDepth++;
        int end = particles.size();
        try {
            for (int index = 0; index < end; index++) {
                IMolangParticleInstance instance = particles.get(index);
                if (!(instance instanceof MolangParticleInstance particle) || !particle.getEmitter().isRemoved()) continue;
                particle.getEmitter().advanceRetainedParticleFrame(deltaNanos);
                particle.advanceSimulation();
            }
        } finally {
            evaluationDepth--;
            if (evaluationDepth == 0 && needsCompaction) compact();
        }
    }

    private void compact() {
        int write = 0;
        for (int read = 0; read < particles.size(); read++) {
            IMolangParticleInstance particle = particles.get(read);
            if (particle != null) {
                particles.set(write, particle);
                positions.put(particle, write++);
            }
        }
        particles.subList(write, particles.size()).clear();
        needsCompaction = false;
    }
}
