package com.kltyton.bedrockruntime.animation.transition;

import java.util.Objects;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.kltyton.bedrockruntime.animation.pose.PoseBlender;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;

/**
 * Pose blending helpers for transitions.
 *
 * <p>The velocity-aware variant accepts caller-supplied velocity poses; it never estimates
 * velocity from frame history. Rotation tangents are parent-space angular velocity vectors
 * in radians per second. Quaternion derivatives are interpolated in four dimensions and
 * normalized, preserving the supplied endpoint angular velocities.</p>
 */
public final class PoseTransition {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private PoseTransition() {
    }

    public static void lerp(PoseBuffer from, PoseBuffer to, double phase, PoseBuffer out) {
        PoseBlender.lerp(from, to, (float) clampPhase(phase), out);
    }

    public static void evaluate(TransitionClock clock, PoseBuffer from, PoseBuffer to, PoseBuffer out) {
        Objects.requireNonNull(clock, "clock");
        lerp(from, to, clock.phase(), out);
    }

    public static void hermite(
            PoseBuffer from,
            PoseBuffer fromVelocity,
            PoseBuffer to,
            PoseBuffer toVelocity,
            double durationSeconds,
            double phase,
            PoseBuffer out) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(fromVelocity, "fromVelocity");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(toVelocity, "toVelocity");
        Objects.requireNonNull(out, "out");
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0.0) {
            throw new IllegalArgumentException("Transition duration must be finite and non-negative");
        }
        float t = (float) clampPhase(phase);
        requireCompatible(out, from);
        requireCompatible(out, fromVelocity);
        requireCompatible(out, to);
        requireCompatible(out, toVelocity);
        if (durationSeconds == 0.0 || t >= 1.0f) {
            out.copyFrom(to);
            return;
        }
        if (t <= 0.0f) {
            out.copyFrom(from);
            return;
        }
        float t2 = t * t;
        float t3 = t2 * t;
        float h00 = 2.0f * t3 - 3.0f * t2 + 1.0f;
        float h10 = t3 - 2.0f * t2 + t;
        float h01 = -2.0f * t3 + 3.0f * t2;
        float h11 = t3 - t2;
        double duration = durationSeconds;
        int size = out.size();

        for (int i = 0; i < size; i++) {
            boolean hasTranslation = from.hasTranslation(i) || fromVelocity.hasTranslation(i)
                    || to.hasTranslation(i) || toVelocity.hasTranslation(i);
            boolean hasRotation = from.hasRotation(i) || fromVelocity.hasRotation(i)
                    || to.hasRotation(i) || toVelocity.hasRotation(i);
            boolean hasScale = from.hasScale(i) || fromVelocity.hasScale(i)
                    || to.hasScale(i) || toVelocity.hasScale(i);

            if (hasTranslation) {
                float x = hermite(
                        from.hasTranslation(i) ? from.translationX(i) : 0.0f,
                        fromVelocity.hasTranslation(i) ? fromVelocity.translationX(i) : 0.0f,
                        to.hasTranslation(i) ? to.translationX(i) : 0.0f,
                        toVelocity.hasTranslation(i) ? toVelocity.translationX(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                float y = hermite(
                        from.hasTranslation(i) ? from.translationY(i) : 0.0f,
                        fromVelocity.hasTranslation(i) ? fromVelocity.translationY(i) : 0.0f,
                        to.hasTranslation(i) ? to.translationY(i) : 0.0f,
                        toVelocity.hasTranslation(i) ? toVelocity.translationY(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                float z = hermite(
                        from.hasTranslation(i) ? from.translationZ(i) : 0.0f,
                        fromVelocity.hasTranslation(i) ? fromVelocity.translationZ(i) : 0.0f,
                        to.hasTranslation(i) ? to.translationZ(i) : 0.0f,
                        toVelocity.hasTranslation(i) ? toVelocity.translationZ(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                out.setTranslation(i, x, y, z);
            } else {
                out.clearTranslation(i);
            }

            if (hasRotation) {
                Scratch scratch = SCRATCH.get();
                if (from.hasRotation(i)) {
                    from.rotationQuaternion(i, scratch.q0);
                } else {
                    scratch.q0.rotationZYX(
                            bindRotZ(from.binding(), i),
                            bindRotY(from.binding(), i),
                            bindRotX(from.binding(), i));
                }
                if (to.hasRotation(i)) {
                    to.rotationQuaternion(i, scratch.q1);
                } else {
                    scratch.q1.rotationZYX(
                            bindRotZ(to.binding(), i),
                            bindRotY(to.binding(), i),
                            bindRotX(to.binding(), i));
                }
                if (scratch.q0.dot(scratch.q1) < 0.0f) {
                    scratch.q1.set(-scratch.q1.x, -scratch.q1.y, -scratch.q1.z, -scratch.q1.w);
                }
                quaternionHermite(scratch.q0, scratch.q1, fromVelocity, toVelocity, i, durationSeconds,
                        h00, h10, h01, h11, scratch.blended);
                scratch.blended.getEulerAnglesZYX(scratch.euler);
                out.setRotation(i, scratch.euler.x, scratch.euler.y, scratch.euler.z);
            } else {
                out.clearRotation(i);
            }

            if (hasScale) {
                float x = hermite(
                        from.hasScale(i) ? from.scaleX(i) : 1.0f,
                        fromVelocity.hasScale(i) ? fromVelocity.scaleX(i) : 0.0f,
                        to.hasScale(i) ? to.scaleX(i) : 1.0f,
                        toVelocity.hasScale(i) ? toVelocity.scaleX(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                float y = hermite(
                        from.hasScale(i) ? from.scaleY(i) : 1.0f,
                        fromVelocity.hasScale(i) ? fromVelocity.scaleY(i) : 0.0f,
                        to.hasScale(i) ? to.scaleY(i) : 1.0f,
                        toVelocity.hasScale(i) ? toVelocity.scaleY(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                float z = hermite(
                        from.hasScale(i) ? from.scaleZ(i) : 1.0f,
                        fromVelocity.hasScale(i) ? fromVelocity.scaleZ(i) : 0.0f,
                        to.hasScale(i) ? to.scaleZ(i) : 1.0f,
                        toVelocity.hasScale(i) ? toVelocity.scaleZ(i) : 0.0f,
                        h00, h10, h01, h11, duration);
                out.setScale(i, x, y, z);
            } else {
                out.clearScale(i);
            }
        }
    }

    public static void hermite(
            TransitionClock clock,
            PoseBuffer from,
            PoseBuffer fromVelocity,
            PoseBuffer to,
            PoseBuffer toVelocity,
            PoseBuffer out) {
        Objects.requireNonNull(clock, "clock");
        hermite(from, fromVelocity, to, toVelocity, clock.duration(), clock.phase(), out);
    }

    private static float hermite(float p0, float v0, float p1, float v1,
            float h00, float h10, float h01, float h11, double duration) {
        return (float) (p0 * h00 + v0 * duration * h10 + p1 * h01 + v1 * duration * h11);
    }

    private static void quaternionHermite(Quaternionf q0, Quaternionf q1, PoseBuffer v0, PoseBuffer v1, int bone,
                                          double duration, double h00, double h10, double h01, double h11,
                                          Quaternionf out) {
        double ax = v0.hasRotation(bone) ? v0.rotationX(bone) : 0;
        double ay = v0.hasRotation(bone) ? v0.rotationY(bone) : 0;
        double az = v0.hasRotation(bone) ? v0.rotationZ(bone) : 0;
        double bx = v1.hasRotation(bone) ? v1.rotationX(bone) : 0;
        double by = v1.hasRotation(bone) ? v1.rotationY(bone) : 0;
        double bz = v1.hasRotation(bone) ? v1.rotationZ(bone) : 0;
        // Parent-space angular velocity: dq/dt = 0.5 * (omega, 0) * q.
        double x = h00 * q0.x + h01 * q1.x + duration * 0.5
                * (h10 * (ax * q0.w + ay * q0.z - az * q0.y) + h11 * (bx * q1.w + by * q1.z - bz * q1.y));
        double y = h00 * q0.y + h01 * q1.y + duration * 0.5
                * (h10 * (-ax * q0.z + ay * q0.w + az * q0.x) + h11 * (-bx * q1.z + by * q1.w + bz * q1.x));
        double z = h00 * q0.z + h01 * q1.z + duration * 0.5
                * (h10 * (ax * q0.y - ay * q0.x + az * q0.w) + h11 * (bx * q1.y - by * q1.x + bz * q1.w));
        double w = h00 * q0.w + h01 * q1.w - duration * 0.5
                * (h10 * (ax * q0.x + ay * q0.y + az * q0.z) + h11 * (bx * q1.x + by * q1.y + bz * q1.z));
        double maximum = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.max(Math.abs(z), Math.abs(w)));
        if (!Double.isFinite(maximum) || maximum < 1.0E-12) {
            throw new IllegalArgumentException("Hermite rotation has a non-finite or cancelling tangent; reduce angular velocity or use ordinary interpolation");
        }
        x /= maximum;
        y /= maximum;
        z /= maximum;
        w /= maximum;
        double inverseLength = 1 / Math.sqrt(x * x + y * y + z * z + w * w);
        out.set((float) (x * inverseLength), (float) (y * inverseLength), (float) (z * inverseLength), (float) (w * inverseLength));
    }

    private static double clampPhase(double phase) {
        if (!Double.isFinite(phase)) {
            throw new IllegalArgumentException("Transition phase must be finite");
        }
        if (phase <= 0.0) {
            return 0.0;
        }
        if (phase >= 1.0) {
            return 1.0;
        }
        return phase;
    }

    private static void requireCompatible(PoseBuffer out, PoseBuffer other) {
        if (!out.sameSkeleton(other)) {
            throw new IllegalArgumentException("Skeleton '" + out.binding().resource()
                    + "': transition pose uses skeleton '" + other.binding().resource()
                    + "#" + other.binding().generation() + "'");
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
        private final Quaternionf q0 = new Quaternionf();
        private final Quaternionf q1 = new Quaternionf();
        private final Quaternionf blended = new Quaternionf();
        private final Vector3f euler = new Vector3f();
    }
}
