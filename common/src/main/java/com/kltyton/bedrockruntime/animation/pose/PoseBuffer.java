package com.kltyton.bedrockruntime.animation.pose;

import java.util.Arrays;
import java.util.Objects;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Mutable pose storage for one immutable {@link SkeletonBinding}.
 *
 * <p>Each bone has independent translation, rotation and scale channels. Rotation values are
 * Euler angles in radians in ZYX order. A channel is present only when its corresponding flag
 * is set, which lets animation sources supply sparse data without overwriting unrelated bones.</p>
 */
public final class PoseBuffer {
    private final SkeletonBinding binding;
    private final float[] translations;
    private final float[] rotations;
    private final float[] scales;
    private final boolean[] translationFlags;
    private final boolean[] rotationFlags;
    private final boolean[] scaleFlags;

    public PoseBuffer(SkeletonBinding binding) {
        this.binding = Objects.requireNonNull(binding, "binding");
        int size = binding.size();
        this.translations = new float[size * 3];
        this.rotations = new float[size * 3];
        this.scales = new float[size * 3];
        this.translationFlags = new boolean[size];
        this.rotationFlags = new boolean[size];
        this.scaleFlags = new boolean[size];
        resetToBind();
    }

    public SkeletonBinding binding() {
        return binding;
    }

    public int size() {
        return binding.size();
    }

    /**
     * Clears every channel flag and sets channel values to identity.
     */
    public void clear() {
        Arrays.fill(translations, 0.0f);
        Arrays.fill(rotations, 0.0f);
        Arrays.fill(scales, 1.0f);
        Arrays.fill(translationFlags, false);
        Arrays.fill(rotationFlags, false);
        Arrays.fill(scaleFlags, false);
    }

    /**
     * Sets values to the bind pose and clears every channel flag.
     */
    public void resetToBind() {
        int size = binding.size();
        for (int i = 0; i < size; i++) {
            SkeletonBinding.Bone bone = binding.bone(i);
            int offset = i * 3;
            translations[offset] = 0.0f;
            translations[offset + 1] = 0.0f;
            translations[offset + 2] = 0.0f;
            rotations[offset] = bone.bindRotX();
            rotations[offset + 1] = bone.bindRotY();
            rotations[offset + 2] = bone.bindRotZ();
            scales[offset] = 1.0f;
            scales[offset + 1] = 1.0f;
            scales[offset + 2] = 1.0f;
        }
        Arrays.fill(translationFlags, false);
        Arrays.fill(rotationFlags, false);
        Arrays.fill(scaleFlags, false);
    }

    public void copyFrom(PoseBuffer other) {
        Objects.requireNonNull(other, "other");
        if (other == this) {
            return;
        }
        if (!binding.sameSkeleton(other.binding)) {
            throw new IllegalArgumentException("Cannot copy pose from skeleton '"
                    + other.binding.resource() + "#" + other.binding.generation()
                    + "' into skeleton '" + binding.resource() + "#" + binding.generation() + "'");
        }
        System.arraycopy(other.translations, 0, translations, 0, translations.length);
        System.arraycopy(other.rotations, 0, rotations, 0, rotations.length);
        System.arraycopy(other.scales, 0, scales, 0, scales.length);
        System.arraycopy(other.translationFlags, 0, translationFlags, 0, translationFlags.length);
        System.arraycopy(other.rotationFlags, 0, rotationFlags, 0, rotationFlags.length);
        System.arraycopy(other.scaleFlags, 0, scaleFlags, 0, scaleFlags.length);
    }

    public boolean sameSkeleton(PoseBuffer other) {
        return other != null && binding.sameSkeleton(other.binding);
    }

    public boolean hasTranslation(int index) {
        checkIndex(index);
        return translationFlags[index];
    }

    public boolean hasRotation(int index) {
        checkIndex(index);
        return rotationFlags[index];
    }

    public boolean hasScale(int index) {
        checkIndex(index);
        return scaleFlags[index];
    }

    public boolean hasAnyTranslation() {
        for (boolean flag : translationFlags) {
            if (flag) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyRotation() {
        for (boolean flag : rotationFlags) {
            if (flag) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyScale() {
        for (boolean flag : scaleFlags) {
            if (flag) {
                return true;
            }
        }
        return false;
    }

    public void setTranslation(int index, float x, float y, float z) {
        checkIndex(index);
        requireFinite(index, "translation", x, y, z);
        int offset = index * 3;
        translations[offset] = x;
        translations[offset + 1] = y;
        translations[offset + 2] = z;
        translationFlags[index] = true;
    }

    public void setTranslation(int index, Vector3fc value) {
        Objects.requireNonNull(value, "value");
        setTranslation(index, value.x(), value.y(), value.z());
    }

    /**
     * Sets a rotation in Euler radians using ZYX application order.
     */
    public void setRotation(int index, float x, float y, float z) {
        checkIndex(index);
        requireFinite(index, "rotation", x, y, z);
        int offset = index * 3;
        rotations[offset] = x;
        rotations[offset + 1] = y;
        rotations[offset + 2] = z;
        rotationFlags[index] = true;
    }

    public void setRotation(int index, Vector3fc value) {
        Objects.requireNonNull(value, "value");
        setRotation(index, value.x(), value.y(), value.z());
    }

    public void setRotationQuaternion(int index, Quaternionfc value) {
        Objects.requireNonNull(value, "value");
        Vector3f euler = new Vector3f();
        value.getEulerAnglesZYX(euler);
        setRotation(index, euler.x, euler.y, euler.z);
    }

    public void setScale(int index, float x, float y, float z) {
        checkIndex(index);
        requireFinite(index, "scale", x, y, z);
        int offset = index * 3;
        scales[offset] = x;
        scales[offset + 1] = y;
        scales[offset + 2] = z;
        scaleFlags[index] = true;
    }

    public void setScale(int index, Vector3fc value) {
        Objects.requireNonNull(value, "value");
        setScale(index, value.x(), value.y(), value.z());
    }

    public void clearTranslation(int index) {
        checkIndex(index);
        int offset = index * 3;
        translations[offset] = 0.0f;
        translations[offset + 1] = 0.0f;
        translations[offset + 2] = 0.0f;
        translationFlags[index] = false;
    }

    public void clearRotation(int index) {
        checkIndex(index);
        int offset = index * 3;
        rotations[offset] = 0.0f;
        rotations[offset + 1] = 0.0f;
        rotations[offset + 2] = 0.0f;
        rotationFlags[index] = false;
    }

    public void clearScale(int index) {
        checkIndex(index);
        int offset = index * 3;
        scales[offset] = 1.0f;
        scales[offset + 1] = 1.0f;
        scales[offset + 2] = 1.0f;
        scaleFlags[index] = false;
    }

    public void clearBone(int index) {
        clearTranslation(index);
        clearRotation(index);
        clearScale(index);
    }

    public float translationX(int index) {
        checkIndex(index);
        return translations[index * 3];
    }

    public float translationY(int index) {
        checkIndex(index);
        return translations[index * 3 + 1];
    }

    public float translationZ(int index) {
        checkIndex(index);
        return translations[index * 3 + 2];
    }

    public Vector3f translation(int index, Vector3f dest) {
        checkIndex(index);
        return dest.set(translationX(index), translationY(index), translationZ(index));
    }

    public float rotationX(int index) {
        checkIndex(index);
        return rotations[index * 3];
    }

    public float rotationY(int index) {
        checkIndex(index);
        return rotations[index * 3 + 1];
    }

    public float rotationZ(int index) {
        checkIndex(index);
        return rotations[index * 3 + 2];
    }

    public Vector3f rotation(int index, Vector3f dest) {
        checkIndex(index);
        return dest.set(rotationX(index), rotationY(index), rotationZ(index));
    }

    public Quaternionf rotationQuaternion(int index, Quaternionf dest) {
        checkIndex(index);
        return dest.rotationZYX(rotationZ(index), rotationY(index), rotationX(index));
    }

    public float scaleX(int index) {
        checkIndex(index);
        return scales[index * 3];
    }

    public float scaleY(int index) {
        checkIndex(index);
        return scales[index * 3 + 1];
    }

    public float scaleZ(int index) {
        checkIndex(index);
        return scales[index * 3 + 2];
    }

    public Vector3f scale(int index, Vector3f dest) {
        checkIndex(index);
        return dest.set(scaleX(index), scaleY(index), scaleZ(index));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= binding.size()) {
            throw new IllegalArgumentException("Skeleton '" + binding.resource()
                    + "': bone index " + index + " is outside [0, " + binding.size() + ")");
        }
    }

    private void requireFinite(int index, String channel, float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Skeleton '" + binding.resource()
                    + "': bone " + index + " " + channel + " must be finite");
        }
    }

    @Override
    public String toString() {
        return "PoseBuffer[" + binding.resource() + "#" + binding.generation() + ", size=" + size() + "]";
    }
}
