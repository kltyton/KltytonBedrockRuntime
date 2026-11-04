package com.kltyton.bedrockruntime.client.particle.runtime.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.RegisterCustomParticleTypeEvent;
import com.kltyton.bedrockruntime.mixin.client.particle.MutableSpriteSetPreviewAccessor;
import com.kltyton.bedrockruntime.mixin.client.particle.ParticleEnginePreviewAccessor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Owns a native particle engine while sharing the world's particle atlas and sprite bindings. */
public final class NativePreviewParticleEngine {
    private static final List<WeakReference<NativePreviewParticleEngine>> PREVIEW_ENGINES = new ArrayList<>();
    private final ParticleEngine engine;
    private final ParticleEngine worldEngine;
    private final PreviewParticleScope.RegistrationState registrationState;
    private ClientLevel level;
    private boolean clearing;

    public NativePreviewParticleEngine(ClientLevel level, ParticleEngine worldEngine, TextureManager textureManager) {
        this.level = level;
        this.worldEngine = worldEngine;
        ParticleEnginePreviewAccessor world = (ParticleEnginePreviewAccessor) worldEngine;
        TextureAtlas atlas = world.kltyton_bedrock_runtime$getTextureAtlas();

        PreviewParticleScope.RegistrationToken registrationToken = PreviewParticleScope.beginNativeEngineConstruction();
        PreviewParticleScope.enterAtlasConstruction(atlas);
        ParticleEngine created;
        PreviewParticleScope.RegistrationState capturedRegistrationState;
        try {
            created = new ParticleEngine(level, textureManager);
        } finally {
            try {
                PreviewParticleScope.exitAtlasConstruction();
            } finally {
                capturedRegistrationState = PreviewParticleScope.endNativeEngineConstruction(registrationToken);
            }
        }
        this.registrationState = capturedRegistrationState;

        ParticleEnginePreviewAccessor preview = (ParticleEnginePreviewAccessor) created;
        preview.kltyton_bedrock_runtime$setTextureAtlas(atlas);
        world.kltyton_bedrock_runtime$getProviders().forEach((key, provider) ->
                preview.kltyton_bedrock_runtime$getProviders().putIfAbsent(key, provider));
        rebindSpriteSets(preview, world);
        this.engine = created;
        synchronized (PREVIEW_ENGINES) {
            PREVIEW_ENGINES.removeIf(reference -> reference.get() == null);
            PREVIEW_ENGINES.add(new WeakReference<>(this));
        }
        PreviewParticleScope.withRegistrationState(registrationState, RegisterCustomParticleTypeEvent::rebindSprites);
    }

    public ParticleEngine engine() { return engine; }
    public ClientLevel level() { return level; }
    public PreviewParticleScope.RegistrationState registrationState() { return registrationState; }

    public void setLevel(ClientLevel level) {
        if (this.level != level) {
            clear();
            this.level = level;
            engine.setLevel(level);
        }
    }

    public void add(Particle particle) { engine.add(particle); }
    public void tick() { engine.tick(); }
    public void clear() {
        if (clearing) return;
        clearing = true;
        RuntimeException failure = null;
        try {
            ParticleEnginePreviewAccessor accessor = (ParticleEnginePreviewAccessor) engine;
            while (true) {
                List<Particle> particles = new ArrayList<>();
                Particle particle;
                while ((particle = accessor.kltyton_bedrock_runtime$getParticlesToAdd().poll()) != null) particles.add(particle);
                for (Queue<Particle> group : new ArrayList<>(accessor.kltyton_bedrock_runtime$getParticles().values())) {
                    while ((particle = group.poll()) != null) particles.add(particle);
                }
                List<Particle> tracked = new ArrayList<>();
                while ((particle = accessor.kltyton_bedrock_runtime$getTrackingEmitters().poll()) != null) tracked.add(particle);
                if (particles.isEmpty() && tracked.isEmpty()) break;
                for (Particle current : particles) {
                    current.getParticleGroup().ifPresent(group -> accessor.kltyton_bedrock_runtime$updateGroupCount(group, -1));
                    try {
                        if (current.isAlive()) current.remove();
                    } catch (RuntimeException exception) {
                        if (failure == null) failure = exception;
                        else failure.addSuppressed(exception);
                    }
                }
                for (Particle current : tracked) {
                    try {
                        if (current.isAlive()) current.remove();
                    } catch (RuntimeException exception) {
                        if (failure == null) failure = exception;
                        else failure.addSuppressed(exception);
                    }
                }
            }
            engine.setLevel(level);
        } finally {
            clearing = false;
        }
        if (failure != null) throw failure;
    }

    public int particleCount() {
        ParticleEnginePreviewAccessor accessor = (ParticleEnginePreviewAccessor) engine;
        int count = accessor.kltyton_bedrock_runtime$getParticlesToAdd().size();
        for (Queue<Particle> particles : accessor.kltyton_bedrock_runtime$getParticles().values()) count += particles.size();
        return count;
    }

    public static void rebindWorldAtlases() {
        PreviewParticleScope.RegistrationState worldRegistrations = PreviewParticleScope.captureRegistrationState();
        synchronized (PREVIEW_ENGINES) {
            Iterator<WeakReference<NativePreviewParticleEngine>> iterator = PREVIEW_ENGINES.iterator();
            while (iterator.hasNext()) {
                NativePreviewParticleEngine preview = iterator.next().get();
                if (preview == null) iterator.remove();
                else {
                    PreviewParticleScope.copyBoundSpriteIds(preview.registrationState, worldRegistrations);
                    preview.rebindSpriteSetsFromWorld();
                    PreviewParticleScope.withRegistrationState(preview.registrationState, RegisterCustomParticleTypeEvent::rebindSprites);
                }
            }
        }
    }

    private void rebindSpriteSetsFromWorld() {
        rebindSpriteSets((ParticleEnginePreviewAccessor) engine, (ParticleEnginePreviewAccessor) worldEngine);
    }

    private static void rebindSpriteSets(ParticleEnginePreviewAccessor preview, ParticleEnginePreviewAccessor world) {
        Map<ResourceLocation, Object> previewSets = preview.kltyton_bedrock_runtime$getSpriteSets();
        for (Map.Entry<ResourceLocation, Object> entry : world.kltyton_bedrock_runtime$getSpriteSets().entrySet()) {
            Object previewSet = previewSets.putIfAbsent(entry.getKey(), entry.getValue());
            if (previewSet != null && previewSet != entry.getValue()) {
                @Nullable List<net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites =
                        ((MutableSpriteSetPreviewAccessor) entry.getValue()).kltyton_bedrock_runtime$getSprites();
                if (sprites != null) ((MutableSpriteSetPreviewAccessor) previewSet).kltyton_bedrock_runtime$rebind(List.copyOf(sprites));
            }
        }
    }

    public void render(LightTexture lightTexture, Camera camera, float partialTick, @Nullable PoseStack poseStack) {
        PoseStack matrices = poseStack == null ? new PoseStack() : poseStack;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        engine.render(matrices, buffers, lightTexture, camera, partialTick);
    }
}
