package com.kltyton.bedrockruntime.client.particle.molang.query;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Captures Fabric model output through the active renderer's public mesh API. */
final class SurfaceParticleModelSampler {
    private SurfaceParticleModelSampler() {}

    static TextureAtlasSprite sample(BlockState state, ClientLevel level, BlockPos pos, Direction face) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if (model instanceof FabricBakedModel fabric && !fabric.isVanillaAdapter()) {
            CaptureContext context = new CaptureContext(state, pos);
            fabric.emitBlockQuads(level, state, pos, () -> RandomSource.create(state.getSeed(pos)), context);
            List<BakedQuad>[] quads = ModelHelper.toQuadLists(context.build());
            for (BakedQuad quad : quads[ModelHelper.toFaceIndex(face)]) return quad.getSprite();
            for (BakedQuad quad : quads[ModelHelper.toFaceIndex(null)]) {
                if (quad.getDirection() == face) return quad.getSprite();
            }
            return model.getParticleIcon();
        }
        RandomSource random = RandomSource.create(state.getSeed(pos));
        for (BakedQuad quad : model.getQuads(state, face, random)) return quad.getSprite();
        random.setSeed(state.getSeed(pos));
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            if (quad.getDirection() == face) return quad.getSprite();
        }
        return model.getParticleIcon();
    }

    private static final class CaptureContext implements RenderContext {
        private final MeshBuilder output;
        private final TransformingEmitter emitter;
        private final List<QuadTransform> transforms = new ArrayList<>();
        private final BlockState state;
        private final BlockPos pos;
        private final RenderMaterial vanillaMaterial;

        CaptureContext(BlockState state, BlockPos pos) {
            Renderer renderer = Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer(),
                    "An active Fabric renderer is required to sample an enhanced block model");
            output = renderer.meshBuilder();
            emitter = new TransformingEmitter(renderer.meshBuilder(), this);
            vanillaMaterial = renderer.materialFinder().find();
            this.state = state;
            this.pos = pos;
        }

        Mesh build() {
            return output.build();
        }

        void append(QuadView quad) {
            QuadEmitter target = output.getEmitter();
            target.copyFrom(quad);
            for (int index = transforms.size() - 1; index >= 0; index--) {
                if (!transforms.get(index).transform(target)) return;
            }
            target.emit();
        }

        @Override public QuadEmitter getEmitter() { return emitter; }
        @Override public boolean hasTransform() { return !transforms.isEmpty(); }
        @Override public void pushTransform(QuadTransform transform) {
            transforms.add(Objects.requireNonNull(transform));
        }
        @Override public void popTransform() {
            transforms.remove(transforms.size() - 1);
        }
        @Override public boolean isFaceCulled(@Nullable Direction face) { return false; }

        private void emitVanilla(BakedModel model, @Nullable BlockState queryState) {
            long seed = state.getSeed(pos);
            for (int face = 0; face <= Direction.values().length; face++) {
                Direction direction = face == Direction.values().length ? null : Direction.values()[face];
                for (BakedQuad quad : model.getQuads(queryState, direction, RandomSource.create(seed))) {
                    emitter.fromVanilla(quad, vanillaMaterial, direction).emit();
                }
            }
        }

        @Override public BakedModelConsumer bakedModelConsumer() {
            return new BakedModelConsumer() {
                @Override public void accept(BakedModel model) { emitVanilla(model, state); }
                @Override public void accept(BakedModel model, @Nullable BlockState stateOverride) {
                    emitVanilla(model, stateOverride);
                }
            };
        }
    }

    /** Keeps fluent calls on this emitter so every quad reaches the transform stack once. */
    private static final class TransformingEmitter implements QuadEmitter {
        private final MeshBuilder input;
        private final CaptureContext context;
        private QuadEmitter delegate;

        TransformingEmitter(MeshBuilder input, CaptureContext context) {
            this.input = input;
            this.context = context;
            delegate = input.getEmitter();
        }

        @Override public QuadEmitter emit() {
            delegate.emit();
            Mesh quad = input.build();
            delegate = input.getEmitter();
            quad.forEach(context::append);
            return this;
        }

        @Override public QuadEmitter pos(int vertexIndex, float x, float y, float z) {
            delegate.pos(vertexIndex, x, y, z);
            return this;
        }

        @Override public QuadEmitter color(int vertexIndex, int color) {
            delegate.color(vertexIndex, color);
            return this;
        }

        @Override public QuadEmitter uv(int vertexIndex, float u, float v) {
            delegate.uv(vertexIndex, u, v);
            return this;
        }

        @Override public QuadEmitter spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
            delegate.spriteBake(sprite, bakeFlags);
            return this;
        }

        @Override public QuadEmitter lightmap(int vertexIndex, int lightmap) {
            delegate.lightmap(vertexIndex, lightmap);
            return this;
        }

        @Override public QuadEmitter normal(int vertexIndex, float x, float y, float z) {
            delegate.normal(vertexIndex, x, y, z);
            return this;
        }

        @Override public QuadEmitter cullFace(@Nullable Direction face) {
            delegate.cullFace(face);
            return this;
        }

        @Override public QuadEmitter nominalFace(@Nullable Direction face) {
            delegate.nominalFace(face);
            return this;
        }

        @Override public QuadEmitter material(RenderMaterial material) {
            delegate.material(material);
            return this;
        }

        @Override public QuadEmitter colorIndex(int colorIndex) {
            delegate.colorIndex(colorIndex);
            return this;
        }

        @Override public QuadEmitter tag(int tag) {
            delegate.tag(tag);
            return this;
        }

        @Override public QuadEmitter copyFrom(QuadView quad) {
            delegate.copyFrom(quad);
            return this;
        }

        @Override public QuadEmitter fromVanilla(int[] quadData, int startIndex) {
            delegate.fromVanilla(quadData, startIndex);
            return this;
        }

        @Override public QuadEmitter fromVanilla(BakedQuad quad, RenderMaterial material, @Nullable Direction cullFace) {
            delegate.fromVanilla(quad, material, cullFace);
            return this;
        }

        @Override public float x(int vertexIndex) {
            return delegate.x(vertexIndex);
        }

        @Override public float y(int vertexIndex) {
            return delegate.y(vertexIndex);
        }

        @Override public float z(int vertexIndex) {
            return delegate.z(vertexIndex);
        }

        @Override public float posByIndex(int vertexIndex, int coordinateIndex) {
            return delegate.posByIndex(vertexIndex, coordinateIndex);
        }

        @Override public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
            return delegate.copyPos(vertexIndex, target);
        }

        @Override public int color(int vertexIndex) {
            return delegate.color(vertexIndex);
        }

        @Override public float u(int vertexIndex) {
            return delegate.u(vertexIndex);
        }

        @Override public float v(int vertexIndex) {
            return delegate.v(vertexIndex);
        }

        @Override public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
            return delegate.copyUv(vertexIndex, target);
        }

        @Override public int lightmap(int vertexIndex) {
            return delegate.lightmap(vertexIndex);
        }

        @Override public boolean hasNormal(int vertexIndex) {
            return delegate.hasNormal(vertexIndex);
        }

        @Override public float normalX(int vertexIndex) {
            return delegate.normalX(vertexIndex);
        }

        @Override public float normalY(int vertexIndex) {
            return delegate.normalY(vertexIndex);
        }

        @Override public float normalZ(int vertexIndex) {
            return delegate.normalZ(vertexIndex);
        }

        @Override public Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
            return delegate.copyNormal(vertexIndex, target);
        }

        @Override public Direction cullFace() {
            return delegate.cullFace();
        }

        @Override public Direction lightFace() {
            return delegate.lightFace();
        }

        @Override public Direction nominalFace() {
            return delegate.nominalFace();
        }

        @Override public Vector3f faceNormal() {
            return delegate.faceNormal();
        }

        @Override public RenderMaterial material() {
            return delegate.material();
        }

        @Override public int colorIndex() {
            return delegate.colorIndex();
        }

        @Override public int tag() {
            return delegate.tag();
        }

        @Override public void toVanilla(int[] target, int targetIndex) {
            delegate.toVanilla(target, targetIndex);
        }

    }
}
