package com.kltyton.bedrockruntime.client.particle.runtime.render;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.BitSet;

/** Carries billboard dimensions and basis vectors through native quad storage. */
public final class BillboardQuadDimensions {
    private static final ThreadLocal<Frames> CURRENT = new ThreadLocal<>();

    private BillboardQuadDimensions() {
    }

    public interface Access {
        void kltyton_bedrock_runtime$addBillboard(ParticleRenderType layer, float x, float y, float z,
                                       Quaternionf orientation, float width, float height,
                                       float u0, float u1, float v0, float v1, int color, int light);

        void kltyton_bedrock_runtime$addOrientedBillboard(ParticleRenderType layer, float x, float y, float z,
                                               Matrix3fc basis, float width, float height,
                                               float u0, float u1, float v0, float v1, int color, int light);
    }

    public interface StorageAccess {
        void kltyton_bedrock_runtime$setBillboardHeight(float height);
        void kltyton_bedrock_runtime$setBillboardBasis(Matrix3fc basis, boolean backFace);
    }

    public static boolean isActive() {
        Frames frames = CURRENT.get();
        return frames != null && frames.depth > 0;
    }

    public static void enter(@Nullable float[] heights, @Nullable BitSet rectangles) {
        enter(heights, rectangles, null, null);
    }

    public static void enter(@Nullable float[] heights, @Nullable BitSet rectangles,
                             @Nullable float[] axes, @Nullable BitSet oriented) {
        Frames frames = CURRENT.get();
        if (frames == null) {
            frames = new Frames();
            CURRENT.set(frames);
        }
        int index = frames.depth;
        if (index == frames.indices.length) frames.grow();
        frames.heights[index] = heights;
        frames.rectangles[index] = rectangles;
        frames.axes[index] = axes;
        frames.oriented[index] = oriented;
        frames.indices[index] = 0;
        frames.selected[index] = false;
        frames.basisSelected[index] = false;
        frames.depth++;
    }

    public static void exit() {
        Frames frames = CURRENT.get();
        int index = --frames.depth;
        frames.heights[index] = null;
        frames.rectangles[index] = null;
        frames.axes[index] = null;
        frames.oriented[index] = null;
        frames.selected[index] = false;
        frames.basisSelected[index] = false;
    }

    public static float selectNext(float width) {
        Frames frames = CURRENT.get();
        if (frames != null && frames.depth > 0) {
            int frame = frames.depth - 1;
            int index = frames.indices[frame]++;
            BitSet rectangles = frames.rectangles[frame];
            frames.selected[frame] = rectangles != null && rectangles.get(index);
            if (frames.selected[frame]) frames.height[frame] = frames.heights[frame][index];
            BitSet oriented = frames.oriented[frame];
            frames.basisSelected[frame] = oriented != null && oriented.get(index);
            frames.basisIndex[frame] = index * 6;
        }
        return width;
    }

    public static float scaleY(float y, float width) {
        Frames frames = CURRENT.get();
        if (frames == null || frames.depth == 0 || !frames.selected[frames.depth - 1]) return y;
        return y * (frames.height[frames.depth - 1] / width);
    }

    public static Vector3f applyBasis(Vector3f point, Quaternionfc rotation,
                                      float horizontal, float vertical, float width) {
        Frames frames = CURRENT.get();
        if (frames == null || frames.depth == 0) return point;
        int frame = frames.depth - 1;
        if (!frames.basisSelected[frame]) return point;
        float[] axes = frames.axes[frame];
        int index = frames.basisIndex[frame];
        float x = horizontal * width;
        float y = vertical * (frames.selected[frame] ? frames.height[frame] : width);
        return point.set(axes[index] * x + axes[index + 3] * y,
                axes[index + 1] * x + axes[index + 4] * y,
                axes[index + 2] * x + axes[index + 5] * y).rotate(rotation);
    }

    private static final class Frames {
        private float[][] heights = new float[4][];
        private BitSet[] rectangles = new BitSet[4];
        private float[][] axes = new float[4][];
        private BitSet[] oriented = new BitSet[4];
        private int[] basisIndex = new int[4];
        private boolean[] basisSelected = new boolean[4];
        private int[] indices = new int[4];
        private float[] height = new float[4];
        private boolean[] selected = new boolean[4];
        private int depth;

        private void grow() {
            int capacity = indices.length * 2;
            heights = Arrays.copyOf(heights, capacity);
            rectangles = Arrays.copyOf(rectangles, capacity);
            axes = Arrays.copyOf(axes, capacity);
            oriented = Arrays.copyOf(oriented, capacity);
            basisIndex = Arrays.copyOf(basisIndex, capacity);
            basisSelected = Arrays.copyOf(basisSelected, capacity);
            indices = Arrays.copyOf(indices, capacity);
            height = Arrays.copyOf(height, capacity);
            selected = Arrays.copyOf(selected, capacity);
        }
    }
}
