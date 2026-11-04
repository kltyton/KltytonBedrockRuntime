package com.kltyton.bedrockruntime.animation.pose;

import java.util.Objects;

import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * Blend operations shared by graphs, blend spaces and transitions.
 *
 * <p>Missing channels use the bind neutral for rotation and identity for translation/scale.
 * Ordinary rotation interpolation reuses JOML's shortest-path nlerp primitive; sparse channel
 * handling and bind/additive rules are owned here. Additive composition deliberately uses Euler
 * addition to match the host GeckoLib additive convention; its {@code additive} input must be a
 * true bind-relative delta (translation bind zero, rotation bind-relative, scale bind one).</p>
 */
public final class PoseBlender {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private PoseBlender() {
    }

    public static void lerp(PoseBuffer from, PoseBuffer to, float weight, PoseBuffer out) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(out, "out");
        requireCompatible(out, from);
        requireCompatible(out, to);
        float w = clamp01(weight);
        int size = out.size();
        for (int i = 0; i < size; i++) {
            lerpBone(from, to, w, i, out);
        }
    }

    public static void lerpMasked(PoseBuffer from, PoseBuffer to, BoneMask mask, float weight, PoseBuffer out) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(mask, "mask");
        Objects.requireNonNull(out, "out");
        requireCompatible(out, from);
        requireCompatible(out, to);
        requireCompatible(out, mask.binding());
        float global = clamp01(weight);
        int size = out.size();
        for (int i = 0; i < size; i++) {
            float w = clamp01(global * mask.weight(i));
            if (w <= 0.0f) {
                copyChannels(from, i, out);
            } else if (w >= 1.0f) {
                copyChannels(to, i, out);
            } else {
                lerpBone(from, to, w, i, out);
            }
        }
    }

    public static void additive(PoseBuffer base, PoseBuffer additive, float weight, PoseBuffer out) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(additive, "additive");
        Objects.requireNonNull(out, "out");
        requireCompatible(out, base);
        requireCompatible(out, additive);
        if (!Float.isFinite(weight)) {
            throw new IllegalArgumentException("Additive weight must be finite");
        }
        int size = out.size();
        for (int i = 0; i < size; i++) {
            additiveBone(base, additive, weight, i, out);
        }
    }

    public static void additiveMasked(PoseBuffer base, PoseBuffer additive, BoneMask mask, float weight,
            PoseBuffer out) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(additive, "additive");
        Objects.requireNonNull(mask, "mask");
        Objects.requireNonNull(out, "out");
        requireCompatible(out, base);
        requireCompatible(out, additive);
        requireCompatible(out, mask.binding());
        if (!Float.isFinite(weight)) {
            throw new IllegalArgumentException("Additive weight must be finite");
        }
        int size = out.size();
        for (int i = 0; i < size; i++) {
            additiveBone(base, additive, weight * mask.weight(i), i, out);
        }
    }

    public static void blendWeighted(PoseBuffer[] poses, float[] weights, int count, PoseBuffer out) {
        Objects.requireNonNull(poses, "poses");
        Objects.requireNonNull(weights, "weights");
        Objects.requireNonNull(out, "out");
        if (count < 1 || count > poses.length || count > weights.length) {
            throw new IllegalArgumentException("Invalid weighted blend count " + count);
        }
        double sum = 0.0;
        for (int k = 0; k < count; k++) {
            PoseBuffer pose = Objects.requireNonNull(poses[k], "poses[" + k + "]");
            requireCompatible(out, pose);
            float value = weights[k];
            if (!Float.isFinite(value) || value < 0.0f) {
                throw new IllegalArgumentException("Blend weight " + k + " must be finite and non-negative");
            }
            sum += value;
        }
        if (sum <= 0.0) {
            out.clear();
            return;
        }
        out.clear();
        Scratch scratch = SCRATCH.get();
        int size = out.size();
        for (int i = 0; i < size; i++) {
            boolean hasTranslation = false;
            boolean hasRotation = false;
            boolean hasScale = false;
            for (int k = 0; k < count; k++) {
                if (weights[k] == 0.0f) {
                    continue;
                }
                PoseBuffer pose = poses[k];
                hasTranslation |= pose.hasTranslation(i);
                hasRotation |= pose.hasRotation(i);
                hasScale |= pose.hasScale(i);
            }

            if (hasTranslation) {
                double x = 0.0;
                double y = 0.0;
                double z = 0.0;
                for (int k = 0; k < count; k++) {
                    PoseBuffer pose = poses[k];
                    double weight = weights[k];
                    if (pose.hasTranslation(i)) {
                        x += pose.translationX(i) * weight;
                        y += pose.translationY(i) * weight;
                        z += pose.translationZ(i) * weight;
                    }
                }
                out.setTranslation(i, (float) (x / sum), (float) (y / sum), (float) (z / sum));
            } else {
                out.clearTranslation(i);
            }

            if (hasRotation) {
                float ax = 0.0f;
                float ay = 0.0f;
                float az = 0.0f;
                float aw = 0.0f;
                boolean hasReference = false;
                for (int k = 0; k < count; k++) {
                    float weight = weights[k];
                    if (weight == 0.0f) {
                        continue;
                    }
                    Quaternionf q = scratch.qb;
                    if (poses[k].hasRotation(i)) {
                        poses[k].rotationQuaternion(i, q);
                    } else {
                        SkeletonBinding binding = poses[k].binding();
                        q.rotationZYX(
                                bindRotZ(binding, i),
                                bindRotY(binding, i),
                                bindRotX(binding, i));
                    }
                    if (!hasReference) {
                        scratch.qReference.set(q);
                        hasReference = true;
                    }
                    float dot = scratch.qReference.x * q.x
                            + scratch.qReference.y * q.y
                            + scratch.qReference.z * q.z
                            + scratch.qReference.w * q.w;
                    float sign = dot < 0.0f ? -weight : weight;
                    ax += q.x * sign;
                    ay += q.y * sign;
                    az += q.z * sign;
                    aw += q.w * sign;
                }
                if (hasReference) {
                    scratch.qAccum.set(ax, ay, az, aw);
                    if (scratch.qAccum.lengthSquared() < 1.0e-12f) {
                        out.setRotation(i, 0.0f, 0.0f, 0.0f);
                    } else {
                        scratch.qAccum.normalize();
                        scratch.qAccum.getEulerAnglesZYX(scratch.euler);
                        out.setRotation(i, scratch.euler.x, scratch.euler.y, scratch.euler.z);
                    }
                } else {
                    out.clearRotation(i);
                }
            } else {
                out.clearRotation(i);
            }

            if (hasScale) {
                double x = 0.0;
                double y = 0.0;
                double z = 0.0;
                for (int k = 0; k < count; k++) {
                    PoseBuffer pose = poses[k];
                    double weight = weights[k];
                    x += (pose.hasScale(i) ? pose.scaleX(i) : 1.0f) * weight;
                    y += (pose.hasScale(i) ? pose.scaleY(i) : 1.0f) * weight;
                    z += (pose.hasScale(i) ? pose.scaleZ(i) : 1.0f) * weight;
                }
                out.setScale(i, (float) (x / sum), (float) (y / sum), (float) (z / sum));
            } else {
                out.clearScale(i);
            }
        }
    }

    public static void mask(PoseBuffer source, BoneMask mask, PoseBuffer out) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mask, "mask");
        Objects.requireNonNull(out, "out");
        requireCompatible(out, source);
        requireCompatible(out, mask.binding());
        Scratch scratch = SCRATCH.get();
        int size = out.size();
        for (int i = 0; i < size; i++) {
            float w = mask.weight(i);
            if (w <= 0.0f) {
                out.clearBone(i);
                continue;
            }
            if (w >= 1.0f) {
                copyChannels(source, i, out);
                continue;
            }
            if (source.hasTranslation(i)) {
                out.setTranslation(i,
                        source.translationX(i) * w,
                        source.translationY(i) * w,
                        source.translationZ(i) * w);
            } else {
                out.clearTranslation(i);
            }
            if (source.hasRotation(i)) {
                source.rotationQuaternion(i, scratch.qa);
                SkeletonBinding binding = source.binding();
                scratch.qIdentity.rotationZYX(
                        bindRotZ(binding, i),
                        bindRotY(binding, i),
                        bindRotX(binding, i));
                Quaternionf blended = scratch.qIdentity.nlerp(scratch.qa, w, scratch.qBlend);
                blended.getEulerAnglesZYX(scratch.euler);
                out.setRotation(i, scratch.euler.x, scratch.euler.y, scratch.euler.z);
            } else {
                out.clearRotation(i);
            }
            if (source.hasScale(i)) {
                out.setScale(i,
                        1.0f + (source.scaleX(i) - 1.0f) * w,
                        1.0f + (source.scaleY(i) - 1.0f) * w,
                        1.0f + (source.scaleZ(i) - 1.0f) * w);
            } else {
                out.clearScale(i);
            }
        }
    }

    private static void additiveBone(PoseBuffer base, PoseBuffer additive, float w, int index, PoseBuffer out) {
        if (w == 0.0f) {
            copyChannels(base, index, out);
            return;
        }
        boolean hasTranslation = base.hasTranslation(index) || additive.hasTranslation(index);
        boolean hasRotation = base.hasRotation(index) || additive.hasRotation(index);
        boolean hasScale = base.hasScale(index) || additive.hasScale(index);

        if (hasTranslation) {
            float bx = base.hasTranslation(index) ? base.translationX(index) : 0.0f;
            float by = base.hasTranslation(index) ? base.translationY(index) : 0.0f;
            float bz = base.hasTranslation(index) ? base.translationZ(index) : 0.0f;
            float ax = additive.hasTranslation(index) ? additive.translationX(index) : 0.0f;
            float ay = additive.hasTranslation(index) ? additive.translationY(index) : 0.0f;
            float az = additive.hasTranslation(index) ? additive.translationZ(index) : 0.0f;
            out.setTranslation(index, bx + ax * w, by + ay * w, bz + az * w);
        } else {
            out.clearTranslation(index);
        }

        if (hasRotation) {
            float bx = base.hasRotation(index) ? base.rotationX(index) : bindRotX(base.binding(), index);
            float by = base.hasRotation(index) ? base.rotationY(index) : bindRotY(base.binding(), index);
            float bz = base.hasRotation(index) ? base.rotationZ(index) : bindRotZ(base.binding(), index);
            float ax = additive.hasRotation(index) ? additive.rotationX(index) : 0.0f;
            float ay = additive.hasRotation(index) ? additive.rotationY(index) : 0.0f;
            float az = additive.hasRotation(index) ? additive.rotationZ(index) : 0.0f;
            out.setRotation(index, bx + ax * w, by + ay * w, bz + az * w);
        } else {
            out.clearRotation(index);
        }

        if (hasScale) {
            float bx = base.hasScale(index) ? base.scaleX(index) : 1.0f;
            float by = base.hasScale(index) ? base.scaleY(index) : 1.0f;
            float bz = base.hasScale(index) ? base.scaleZ(index) : 1.0f;
            float ax = additive.hasScale(index) ? additive.scaleX(index) : 1.0f;
            float ay = additive.hasScale(index) ? additive.scaleY(index) : 1.0f;
            float az = additive.hasScale(index) ? additive.scaleZ(index) : 1.0f;
            out.setScale(index,
                    bx * (1.0f + (ax - 1.0f) * w),
                    by * (1.0f + (ay - 1.0f) * w),
                    bz * (1.0f + (az - 1.0f) * w));
        } else {
            out.clearScale(index);
        }
    }

    private static void lerpBone(PoseBuffer from, PoseBuffer to, float w, int index, PoseBuffer out) {
        boolean hasTranslation = from.hasTranslation(index) || to.hasTranslation(index);
        boolean hasRotation = from.hasRotation(index) || to.hasRotation(index);
        boolean hasScale = from.hasScale(index) || to.hasScale(index);

        if (!hasTranslation) {
            out.clearTranslation(index);
        } else if (w <= 0.0f) {
            if (from.hasTranslation(index)) {
                out.setTranslation(index, from.translationX(index), from.translationY(index), from.translationZ(index));
            } else {
                out.clearTranslation(index);
            }
        } else if (w >= 1.0f) {
            if (to.hasTranslation(index)) {
                out.setTranslation(index, to.translationX(index), to.translationY(index), to.translationZ(index));
            } else {
                out.clearTranslation(index);
            }
        } else {
            float fx = from.hasTranslation(index) ? from.translationX(index) : 0.0f;
            float fy = from.hasTranslation(index) ? from.translationY(index) : 0.0f;
            float fz = from.hasTranslation(index) ? from.translationZ(index) : 0.0f;
            float tx = to.hasTranslation(index) ? to.translationX(index) : 0.0f;
            float ty = to.hasTranslation(index) ? to.translationY(index) : 0.0f;
            float tz = to.hasTranslation(index) ? to.translationZ(index) : 0.0f;
            out.setTranslation(index,
                    fx + (tx - fx) * w,
                    fy + (ty - fy) * w,
                    fz + (tz - fz) * w);
        }

        if (!hasRotation) {
            out.clearRotation(index);
        } else if (w <= 0.0f) {
            if (from.hasRotation(index)) {
                out.setRotation(index, from.rotationX(index), from.rotationY(index), from.rotationZ(index));
            } else {
                out.clearRotation(index);
            }
        } else if (w >= 1.0f) {
            if (to.hasRotation(index)) {
                out.setRotation(index, to.rotationX(index), to.rotationY(index), to.rotationZ(index));
            } else {
                out.clearRotation(index);
            }
        } else {
            Scratch scratch = SCRATCH.get();
            if (from.hasRotation(index)) {
                from.rotationQuaternion(index, scratch.qa);
            } else {
                scratch.qa.rotationZYX(
                        bindRotZ(from.binding(), index),
                        bindRotY(from.binding(), index),
                        bindRotX(from.binding(), index));
            }
            if (to.hasRotation(index)) {
                to.rotationQuaternion(index, scratch.qb);
            } else {
                scratch.qb.rotationZYX(
                        bindRotZ(to.binding(), index),
                        bindRotY(to.binding(), index),
                        bindRotX(to.binding(), index));
            }
            Quaternionf blended = scratch.qa.nlerp(scratch.qb, w, scratch.qBlend);
            blended.getEulerAnglesZYX(scratch.euler);
            out.setRotation(index, scratch.euler.x, scratch.euler.y, scratch.euler.z);
        }

        if (!hasScale) {
            out.clearScale(index);
        } else if (w <= 0.0f) {
            if (from.hasScale(index)) {
                out.setScale(index, from.scaleX(index), from.scaleY(index), from.scaleZ(index));
            } else {
                out.clearScale(index);
            }
        } else if (w >= 1.0f) {
            if (to.hasScale(index)) {
                out.setScale(index, to.scaleX(index), to.scaleY(index), to.scaleZ(index));
            } else {
                out.clearScale(index);
            }
        } else {
            float fx = from.hasScale(index) ? from.scaleX(index) : 1.0f;
            float fy = from.hasScale(index) ? from.scaleY(index) : 1.0f;
            float fz = from.hasScale(index) ? from.scaleZ(index) : 1.0f;
            float tx = to.hasScale(index) ? to.scaleX(index) : 1.0f;
            float ty = to.hasScale(index) ? to.scaleY(index) : 1.0f;
            float tz = to.hasScale(index) ? to.scaleZ(index) : 1.0f;
            out.setScale(index,
                    fx + (tx - fx) * w,
                    fy + (ty - fy) * w,
                    fz + (tz - fz) * w);
        }
    }

    private static void copyChannels(PoseBuffer source, int index, PoseBuffer out) {
        if (source.hasTranslation(index)) {
            out.setTranslation(index, source.translationX(index), source.translationY(index), source.translationZ(index));
        } else {
            out.clearTranslation(index);
        }
        if (source.hasRotation(index)) {
            out.setRotation(index, source.rotationX(index), source.rotationY(index), source.rotationZ(index));
        } else {
            out.clearRotation(index);
        }
        if (source.hasScale(index)) {
            out.setScale(index, source.scaleX(index), source.scaleY(index), source.scaleZ(index));
        } else {
            out.clearScale(index);
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Blend weight must be finite");
        }
        if (value <= 0.0f) {
            return 0.0f;
        }
        if (value >= 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static void requireCompatible(PoseBuffer out, PoseBuffer other) {
        if (!out.sameSkeleton(other)) {
            throw new IllegalArgumentException("Skeleton '" + out.binding().resource()
                    + "': cannot blend pose from skeleton '" + other.binding().resource()
                    + "#" + other.binding().generation() + "'");
        }
    }

    private static void requireCompatible(PoseBuffer out, SkeletonBinding binding) {
        if (!out.binding().sameSkeleton(binding)) {
            throw new IllegalArgumentException("Skeleton '" + out.binding().resource()
                    + "': cannot use mask from skeleton '" + binding.resource()
                    + "#" + binding.generation() + "'");
        }
    }

    private static float bindRotX(SkeletonBinding binding, int index) {
        return binding.bone(index).bindRotX();
    }

    private static float bindRotY(SkeletonBinding binding, int index) {
        return binding.bone(index).bindRotY();
    }

    private static float bindRotZ(SkeletonBinding binding, int index) {
        return binding.bone(index).bindRotZ();
    }

    private static final class Scratch {
        private final Quaternionf qa = new Quaternionf();
        private final Quaternionf qb = new Quaternionf();
        private final Quaternionf qAccum = new Quaternionf();
        private final Quaternionf qReference = new Quaternionf();
        private final Quaternionf qIdentity = new Quaternionf();
        private final Quaternionf qBlend = new Quaternionf();
        private final Vector3f euler = new Vector3f();
    }
}
