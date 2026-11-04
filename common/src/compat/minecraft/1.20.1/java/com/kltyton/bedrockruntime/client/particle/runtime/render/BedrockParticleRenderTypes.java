package com.kltyton.bedrockruntime.client.particle.runtime.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import com.kltyton.bedrockruntime.client.particle.runtime.render.IrisParticleShader;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;

import java.util.LinkedHashSet;
import java.util.List;

/** Native particle batches for Bedrock materials on either Java atlas. */
public final class BedrockParticleRenderTypes {
    public static final ParticleRenderType PARTICLE_OPAQUE = new Sheet(Mode.OPAQUE, TextureAtlas.LOCATION_PARTICLES);
    public static final ParticleRenderType PARTICLE_ALPHA = new Sheet(Mode.ALPHA, TextureAtlas.LOCATION_PARTICLES);
    public static final ParticleRenderType PARTICLE_ADD = new Sheet(Mode.ADD, TextureAtlas.LOCATION_PARTICLES);
    public static final ParticleRenderType PARTICLE_BLEND = new Sheet(Mode.BLEND, TextureAtlas.LOCATION_PARTICLES);
    public static final ParticleRenderType TERRAIN_OPAQUE = new Sheet(Mode.OPAQUE, TextureAtlas.LOCATION_BLOCKS);
    public static final ParticleRenderType TERRAIN_ALPHA = new Sheet(Mode.ALPHA, TextureAtlas.LOCATION_BLOCKS);
    public static final ParticleRenderType TERRAIN_ADD = new Sheet(Mode.ADD, TextureAtlas.LOCATION_BLOCKS);
    public static final ParticleRenderType TERRAIN_BLEND = new Sheet(Mode.BLEND, TextureAtlas.LOCATION_BLOCKS);
    private static final ThreadLocal<DrawScope> DRAW = new ThreadLocal<>();

    private BedrockParticleRenderTypes() {}

    private enum Mode {
        OPAQUE(false), ALPHA(false), ADD(true), BLEND(true);
        final boolean translucent;
        Mode(boolean translucent) { this.translucent = translucent; }
    }

    private record Sheet(Mode mode, net.minecraft.resources.ResourceLocation atlas) implements ParticleRenderType {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(!mode.translucent);
            RenderSystem.setShaderTexture(0, atlas);
            if (mode.translucent) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        mode == Mode.ADD ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            } else {
                RenderSystem.disableBlend();
            }
            ShaderInstance integrated = RenderSystem.getShader();
            RenderSystem.setShader(() -> integrated instanceof IrisParticleShader ? integrated
                    : mode == Mode.ALPHA ? ParticleRenderLayers.particleAlphaShader() : ParticleRenderLayers.particleNoDiscardShader());
            builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public boolean isTranslucent() { return mode.translucent; }

        @Override
        public String toString() {
            return (atlas.equals(TextureAtlas.LOCATION_BLOCKS) ? "TERRAIN_PARTICLE_" : "PARTICLE_") + mode;
        }
    }

    public static boolean owns(ParticleRenderType type) { return type instanceof Sheet; }

    public static ParticleRenderType forMaterial(DescriptionMaterial material, boolean terrain) {
        if (material == DescriptionMaterial.particles_opaque) return terrain ? TERRAIN_OPAQUE : PARTICLE_OPAQUE;
        if (material == DescriptionMaterial.particles_alpha) return terrain ? TERRAIN_ALPHA : PARTICLE_ALPHA;
        if (material == DescriptionMaterial.particles_add) return terrain ? TERRAIN_ADD : PARTICLE_ADD;
        if (material == DescriptionMaterial.particles_blend) return terrain ? TERRAIN_BLEND : PARTICLE_BLEND;
        if (material == DescriptionMaterial.TERRAIN_SHEET) return ParticleRenderType.TERRAIN_SHEET;
        if (material == DescriptionMaterial.PARTICLE_SHEET_OPAQUE) return terrain ? ParticleRenderType.TERRAIN_SHEET : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        if (material == DescriptionMaterial.PARTICLE_SHEET_LIT) return terrain ? ParticleRenderType.TERRAIN_SHEET : ParticleRenderType.PARTICLE_SHEET_LIT;
        if (material == DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT) return terrain ? TERRAIN_BLEND : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        if (material == DescriptionMaterial.CUSTOM) return ParticleRenderType.CUSTOM;
        return ParticleRenderType.NO_RENDER;
    }

    public static List<ParticleRenderType> renderOrder(List<ParticleRenderType> original) {
        LinkedHashSet<ParticleRenderType> order = new LinkedHashSet<>();
        boolean solidAdded = false;
        for (ParticleRenderType type : original) {
            if (!solidAdded && (type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                    || type instanceof Sheet sheet && sheet.mode.translucent)) {
                order.addAll(List.of(PARTICLE_OPAQUE, PARTICLE_ALPHA, TERRAIN_OPAQUE, TERRAIN_ALPHA));
                solidAdded = true;
            }
            order.add(type);
        }
        if (!solidAdded) order.addAll(List.of(PARTICLE_OPAQUE, PARTICLE_ALPHA, TERRAIN_OPAQUE, TERRAIN_ALPHA));
        order.addAll(List.of(PARTICLE_BLEND, PARTICLE_ADD, TERRAIN_BLEND, TERRAIN_ADD));
        return List.copyOf(order);
    }

    public static DrawScope enterDraw(ParticleRenderType type) {
        DrawScope scope = new DrawScope(type instanceof Sheet sheet ? sheet.mode : null, DRAW.get());
        DRAW.set(scope);
        return scope;
    }

    public static void applyDrawState() {
        DrawScope scope = DRAW.get();
        if (scope == null || scope.mode == null) return;
        switch (scope.mode) {
            case OPAQUE, ALPHA -> {
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
            }
            case ADD -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.depthMask(false);
            }
            case BLEND -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.depthMask(false);
            }
        }
    }

    public static float irisAlphaReference(float original) {
        DrawScope scope = DRAW.get();
        return scope == null || scope.mode == null ? original
                : scope.mode == Mode.ALPHA ? Math.nextDown(0.5F) : -1.0F;
    }

    public static final class DrawScope implements AutoCloseable {
        private final @Nullable Mode mode;
        private final @Nullable DrawScope previous;
        private final @Nullable Uniform alpha;
        private final float originalAlpha;

        private DrawScope(@Nullable Mode mode, @Nullable DrawScope previous) {
            this.mode = mode;
            this.previous = previous;
            ShaderInstance shader = RenderSystem.getShader();
            alpha = mode != null && shader instanceof IrisParticleShader iris ? iris.kltyton_bedrock_runtime$alphaTestUniform() : null;
            originalAlpha = alpha == null ? 0.0F : alpha.getFloatBuffer().get(0);
            if (alpha != null) alpha.set(mode == Mode.ALPHA ? Math.nextDown(0.5F) : -1.0F);
        }

        @Override
        public void close() {
            try {
                if (alpha != null) alpha.set(originalAlpha);
            } finally {
                if (previous == null) DRAW.remove();
                else DRAW.set(previous);
            }
        }
    }
}
