package com.kltyton.bedrockruntime.client.particle.model;

import com.kltyton.bedrockruntime.client.particle.api.ParticlePresetLoadedEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomComponentEvent;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleAppearanceModel;
import com.kltyton.bedrockruntime.client.particle.model.platform.ModelParticlePlatform;
import com.kltyton.bedrockruntime.client.particle.model.resource.ModelParticleReloadListener;
import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleModels;
import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.particle.registry.ModelParticleTypes;
import net.minecraft.core.particles.ParticleType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point for model-particle support.
 *
 * <p>The loader wiring must register {@link #modelParticleType()} in the game's
 * {@code PARTICLE_TYPE} registry before clients load resources, then call {@link #initialize()}
 * once from the client bootstrap. This class registers the {@code model_appearance} component and
 * the {@code model_particle} provider through {@link ParticleEvents}.</p>
 */
public final class BedrockModelParticles {
    public static final String NAMESPACE = BedrockParticles.MODID;
    public static final String MODEL_PARTICLE_PATH = "model_particle";
    public static final String MODEL_APPEARANCE_PATH = ParticleAppearanceModel.ID_PATH;
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private BedrockModelParticles() {
    }

    /**
     * Registers the model particle type in the game registry. Loader wiring should call this from
     * its common registry event before client resources are loaded.
     */
    public static void registerParticleType(net.minecraft.core.Registry<ParticleType<?>> registry) {
        ModelParticleTypes.registerParticleType(registry);
    }

    /** The particle type that resources must name in {@code description.type}. */
    public static ParticleType<MolangParticleOption> modelParticleType() {
        return ModelParticleTypes.modelParticleType();
    }

    public static String modelParticleId() {
        return NAMESPACE + ":" + MODEL_PARTICLE_PATH;
    }

    public static String modelAppearanceId() {
        return NAMESPACE + ":" + MODEL_APPEARANCE_PATH;
    }

    /**
     * Registers providers and reload hooks. Safe to call more than once; the first call wins.
     */
    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        ParticleEvents.addListener(RegisterCustomComponentEvent.class, event ->
                event.register(BedrockParticles.asResource(MODEL_APPEARANCE_PATH), ParticleAppearanceModel.CODEC));
        ParticleEvents.addListener(RegisterCustomParticleTypeEvent.class, event ->
                event.register(ModelParticleTypes.modelParticleType(), ModelParticlePlatform::createParticle));
        ParticleEvents.addListener(ParticlePresetLoadedEvent.class, BedrockModelParticles::onPresetLoaded);
    }

    private static void onPresetLoaded(ParticlePresetLoadedEvent event) {
        if (event == null || event.getPreset() == null || event.getPreset().effect == null) {
            return;
        }
        var component = event.getPreset().effect.components.get(
                BedrockParticles.asResource(MODEL_APPEARANCE_PATH));
        if (component instanceof ParticleAppearanceModel appearance) {
            event.getPreset().setTicket(ParticleAppearanceModel.class, appearance);
            if (appearance.faceCameraMode().isPresent()) {
                event.getPreset().facingCameraMode = com.kltyton.bedrockruntime.client.particle.runtime.FaceCameraMode
                        .fromComponent(appearance.faceCameraMode().get());
            }
            var direction = appearance.direction();
            event.getPreset().minSpeedThresholdSqr = direction
                    .equals(com.kltyton.bedrockruntime.client.particle.definition.component.ParticleAppearanceBillboard.Direction.DEFAULT)
                    ? 0.01F : net.minecraft.util.Mth.square(direction.minSpeedThreshold());
        }
    }

    /** Stable reload-listener id for Fabric/loader registration. */
    public static String reloadListenerId() {
        return NAMESPACE + ":model_particles";
    }

    /** Reload listener to register with the client resource manager. */
    public static net.minecraft.server.packs.resources.PreparableReloadListener reloadListener() {
        return ModelParticleReloadListener.INSTANCE;
    }

    public static boolean initialized() {
        return INITIALIZED.get();
    }

    /** Clears registered resource models; programmatic registrations survive. */
    public static void clearResourceModels() {
        ModelParticleModels.clearResourceRegistrations();
    }
}







