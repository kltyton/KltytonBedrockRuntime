package com.kltyton.bedrockruntime.server.collision.solver;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * CN: OBB 接触的速度级顺序冲量求解器。恢复系数固定为 0，避免硬表面的蹦床效应。
 * EN: Velocity-level sequential impulse solver for OBB contacts. Restitution is fixed at zero to avoid trampoline behavior.
 */
public final class ObbSequentialImpulseSolver {
    private static final double EPSILON = 1.0E-10;
    private static final double HARD_SLOP = 0.004;
    private static final double HARD_BAUMGARTE = 0.16;
    private static final double HARD_MAX_BIAS = 0.08;
    private static final double HARD_MAX_IMPULSE = 0.35;
    private static final double HARD_FRICTION = 0.42;
    private static final double HARD_POSITION_PERCENT = 0.16;
    private static final double HARD_SUPPORT_POSITION_PERCENT = 0.08;
    private static final double HARD_MAX_POSITION_CORRECTION = 0.04;
    private static final double HARD_MAX_SUPPORT_CORRECTION = 0.018;
    private static final double SOFT_SLOP = 0.01;
    private static final double SOFT_STIFFNESS = 0.10;
    private static final double SOFT_DAMPING = 0.28;
    private static final double SOFT_MAX_SEPARATION_SPEED = 0.12;

    private ObbSequentialImpulseSolver() {
    }

    public static Solution solveHard(Contact contact, double previousNormalImpulse) {
        Vec3 normal = normalized(contact.normal());
        double inverseMassSum = contact.firstInverseMass() + contact.secondInverseMass();
        if (normal == Vec3.ZERO || inverseMassSum <= EPSILON) {
            return Solution.ZERO;
        }

        Vec3 relativeVelocity = contact.firstVelocity().subtract(contact.secondVelocity());
        double normalVelocity = relativeVelocity.dot(normal);
        double bias = contact.verticalSupport()
                ? 0.0
                : Math.min(HARD_MAX_BIAS,
                        Math.max(0.0, contact.penetration() - HARD_SLOP) * HARD_BAUMGARTE);
        double normalImpulse = Mth.clamp(
                (Math.max(0.0, -normalVelocity) + bias) / inverseMassSum,
                0.0,
                HARD_MAX_IMPULSE);

        Vec3 firstDelta = normal.scale(normalImpulse * contact.firstInverseMass());
        Vec3 secondDelta = normal.scale(-normalImpulse * contact.secondInverseMass());

        Vec3 postNormalRelative = relativeVelocity.add(firstDelta).subtract(secondDelta);
        Vec3 tangentVelocity = postNormalRelative.subtract(normal.scale(postNormalRelative.dot(normal)));
        double tangentSpeed = tangentVelocity.length();
        if (tangentSpeed > EPSILON) {
            double contactLoad = Math.max(normalImpulse, Math.max(0.0, previousNormalImpulse) * 0.5);
            double tangentImpulse = Math.min(tangentSpeed / inverseMassSum, HARD_FRICTION * contactLoad);
            Vec3 tangentDirection = tangentVelocity.scale(-1.0 / tangentSpeed);
            firstDelta = firstDelta.add(tangentDirection.scale(tangentImpulse * contact.firstInverseMass()));
            secondDelta = secondDelta.add(tangentDirection.scale(-tangentImpulse * contact.secondInverseMass()));
        }

        double remainingPenetration = Math.max(0.0, contact.penetration() - HARD_SLOP);
        double positionPercent = contact.verticalSupport()
                ? HARD_SUPPORT_POSITION_PERCENT
                : HARD_POSITION_PERCENT;
        double maxCorrection = contact.verticalSupport()
                ? HARD_MAX_SUPPORT_CORRECTION
                : HARD_MAX_POSITION_CORRECTION;
        double correctionMagnitude = Math.min(maxCorrection, remainingPenetration * positionPercent);
        Vec3 firstCorrection = normal.scale(
                correctionMagnitude * contact.firstInverseMass() / inverseMassSum);
        Vec3 secondCorrection = normal.scale(
                -correctionMagnitude * contact.secondInverseMass() / inverseMassSum);
        return new Solution(firstDelta, secondDelta, firstCorrection, secondCorrection, normalImpulse);
    }

    public static Solution solveSoft(Contact contact) {
        Vec3 normal = normalized(contact.normal());
        double inverseMassSum = contact.firstInverseMass() + contact.secondInverseMass();
        if (normal == Vec3.ZERO || inverseMassSum <= EPSILON) {
            return Solution.ZERO;
        }

        Vec3 relativeVelocity = contact.firstVelocity().subtract(contact.secondVelocity());
        double closingSpeed = Math.max(0.0, -relativeVelocity.dot(normal));
        double penetration = Math.max(0.0, contact.penetration() - SOFT_SLOP);
        double targetSeparationSpeed = Math.min(
                SOFT_MAX_SEPARATION_SPEED,
                penetration * SOFT_STIFFNESS + closingSpeed * SOFT_DAMPING);
        if (targetSeparationSpeed <= EPSILON) {
            return Solution.ZERO;
        }

        double impulse = targetSeparationSpeed / inverseMassSum;
        return new Solution(
                normal.scale(impulse * contact.firstInverseMass()),
                normal.scale(-impulse * contact.secondInverseMass()),
                Vec3.ZERO,
                Vec3.ZERO,
                impulse);
    }

    private static Vec3 normalized(Vec3 value) {
        return value == null || value.lengthSqr() <= EPSILON ? Vec3.ZERO : value.normalize();
    }

    public record Contact(
            Vec3 normal,
            double penetration,
            Vec3 firstVelocity,
            Vec3 secondVelocity,
            double firstInverseMass,
            double secondInverseMass,
            boolean verticalSupport) {

        public Contact {
            normal = normal == null ? Vec3.ZERO : normal;
            firstVelocity = firstVelocity == null ? Vec3.ZERO : firstVelocity;
            secondVelocity = secondVelocity == null ? Vec3.ZERO : secondVelocity;
            penetration = Math.max(0.0, penetration);
            firstInverseMass = Math.max(0.0, firstInverseMass);
            secondInverseMass = Math.max(0.0, secondInverseMass);
        }
    }

    public record Solution(
            Vec3 firstVelocityDelta,
            Vec3 secondVelocityDelta,
            Vec3 firstPositionCorrection,
            Vec3 secondPositionCorrection,
            double normalImpulse) {
        public static final Solution ZERO = new Solution(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 0.0);
    }
}
