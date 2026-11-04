package com.kltyton.bedrockruntime.particle.registry;

import com.kltyton.bedrockruntime.client.particle.api.IComponent;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomComponentEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEmitterTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomEventNodeEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;
import com.kltyton.bedrockruntime.client.particle.api.geckolib.GeckoLibHelper;
import com.kltyton.bedrockruntime.client.particle.definition.component.*;
import com.kltyton.bedrockruntime.client.particle.definition.event.*;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.EmitterAttachHandler;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

/** Registers the built-in Bedrock component and event-node codecs. */
public final class ParticleRegistry {
    private static boolean initialized;

    private ParticleRegistry() {
    }

    public static synchronized void initializeDefaults() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerComponents();
        registerEventNodes();
        ParticleEvents.post(new RegisterCustomComponentEvent());
        ParticleEvents.post(new RegisterCustomEventNodeEvent());
        RegisterCustomParticleTypeEvent.registerDefaults();
        RegisterCustomEmitterTypeEvent.postEvent();
        EmitterAttachHandler.postEvent();
        if (BedrockParticles.GECKOLIB_LOADED) {
            GeckoLibHelper.postEvent();
        }
    }

    private static void registerComponents() {
        IComponent.register("emitter_local_space", EmitterLocalSpace.CODEC);
        IComponent.register("emitter_initialization", EmitterInitialization.CODEC);

        IComponent.register("emitter_rate_instant", EmitterRate.Instant.CODEC);
        IComponent.register("emitter_rate_steady", EmitterRate.Steady.CODEC);
        IComponent.register("emitter_rate_manual", EmitterRate.Manual.CODEC);

        IComponent.register("emitter_lifetime_looping", EmitterLifetime.Looping.CODEC);
        IComponent.register("emitter_lifetime_once", EmitterLifetime.Once.CODEC);
        IComponent.register("emitter_lifetime_expression", EmitterLifetime.Expression.CODEC);
        IComponent.register("emitter_lifetime_events", EmitterLifetimeEvents.CODEC);

        IComponent.register(EmitterShape.Point.ID, EmitterShape.Point.CODEC);
        IComponent.register(EmitterShape.Custom.ID, EmitterShape.Custom.CODEC);
        IComponent.register(EmitterShape.Sphere.ID, EmitterShape.Sphere.CODEC);
        IComponent.register(EmitterShape.Box.ID, EmitterShape.Box.CODEC);
        IComponent.register(EmitterShape.EntityAABB.ID, EmitterShape.EntityAABB.CODEC);
        IComponent.register(EmitterShape.Disc.ID, EmitterShape.Disc.CODEC);
        IComponent.register(BedrockParticles.asResource("emitter_multi_shape"), EmitterMultiShape.CODEC);

        IComponent.register("particle_initial_speed", ParticleInitialSpeed.CODEC);
        IComponent.register("particle_initial_spin", ParticleInitialSpin.CODEC);
        IComponent.register(ParticleInitialization.ID, ParticleInitialization.CODEC);

        IComponent.register(ParticleMotionDynamic.ID, ParticleMotionDynamic.CODEC);
        IComponent.register("particle_motion_parametric", ParticleMotionParametric.CODEC);
        IComponent.register(ParticleMotionCollision.ID, ParticleMotionCollision.CODEC);

        IComponent.register(ParticleAppearanceBillboard.ID, ParticleAppearanceBillboard.CODEC);
        IComponent.register("particle_appearance_tinting", ParticleAppearanceTinting.CODEC);
        IComponent.register("particle_appearance_lighting", ParticleAppearanceLighting.CODEC);
        IComponent.register(ParticleAppearanceCameraDistanceFade.ID, ParticleAppearanceCameraDistanceFade.CODEC);

        IComponent.register("particle_lifetime_expression", ParticleLifetimeExpression.CODEC);
        IComponent.register(ParticleLifeTimeEvents.ID, ParticleLifeTimeEvents.CODEC);
        IComponent.register("particle_kill_plane", ParticleLifetimeKillPlane.CODEC);
        IComponent.register("particle_expire_if_in_blocks", ParticleExpireIfInBlocks.CODEC);
        IComponent.register("particle_expire_if_not_in_blocks", ParticleExpireIfNotInBlocks.CODEC);
    }

    private static void registerEventNodes() {
        IEventNode.register("sequence", EventSequence.CODEC);
        IEventNode.register("weight", EventRandomize.WEIGHT_CODEC);
        IEventNode.register("randomize", EventRandomize.CODEC);
        IEventNode.register("particle_effect", ParticleEffect.CODEC);
        IEventNode.register("sound_effect", SoundEffect.CODEC);
        IEventNode.register("expression", NodeMolangExp.CODEC);
        IEventNode.register("log", EventLog.CODEC);
    }
}
