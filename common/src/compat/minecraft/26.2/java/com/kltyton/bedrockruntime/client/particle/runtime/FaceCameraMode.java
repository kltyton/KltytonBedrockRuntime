package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.definition.MathHelper;
import com.kltyton.bedrockruntime.client.particle.definition.component.ParticleAppearanceBillboard;

public enum FaceCameraMode implements SingleQuadParticle.FacingCameraMode {
    DO_NOTHING {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.identity();
        }
    },
    LOOKAT_XYZ {
        private static final Vector3f wd = new Vector3f();
        private static final Vector3f qd = new Vector3f();
        private static final Vector3f up = new Vector3f(0, 1, 0);
        private static final Vector3f worldPos = new Vector3f();
        private static final Matrix3f mat = new Matrix3f();

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            Vector3f xd = toParticleSpace(instance, camera.position().toVector3f().sub(
                    instance.getRenderWorldPosition(worldPos, partialTick)
            ));
            float length = xd.length();
            if (length <= java.lang.Math.ulp(1.0F)) {
                toParticleSpace(instance, quaternion.identity());
                return;
            }
            xd.div(length);
            up.cross(xd, wd).normalize();
            xd.cross(wd, qd);
            quaternion.setFromNormalized(mat.set(
                    wd.x, qd.x, xd.x,
                    wd.y, qd.y, xd.y,
                    wd.z, qd.z, xd.z
            ).invert());
        }
    },
    LOOKAT_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            Vec3 cameraPos = camera.position();
            Vector3f position = instance.getRenderWorldPosition(TEMP, partialTick);
            float x = (float) cameraPos.x - position.x;
            float z = (float) cameraPos.z - position.z;
            lookAtBasis(DIRECTION_MATRIX, x, 0.0F, z).getNormalizedRotation(quaternion);
            toParticleSpace(instance, quaternion);
        }
    },
    ROTATE_XYZ {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            setRotation(quaternion, camera, partialTick);
            toParticleSpace(instance, quaternion);
        }

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(camera.rotation());
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            setRotation(quaternion, camera, partialTick);
            toParticleSpace(instance, quaternion);
        }

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            Quaternionf rotation = camera.rotation();
            float x = 2.0F * (rotation.x * rotation.z + rotation.w * rotation.y);
            float z = 1.0F - 2.0F * (rotation.x * rotation.x + rotation.y * rotation.y);
            float length = (float) java.lang.Math.sqrt(x * x + z * z);
            if (length <= java.lang.Math.ulp(1.0F)) quaternion.identity();
            else quaternion.rotationY((float) java.lang.Math.atan2(x, z));
        }
    },
    DIRECTION_X {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, Mth.HALF_PI, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionXRotation(instance, quaternion, partialTick)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    DIRECTION_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, Mth.PI, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionYRotation(instance, quaternion, partialTick)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    DIRECTION_Z {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionZRotation(instance, quaternion, partialTick)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    LOOKAT_DIRECTION {
        private static final Vector3f X = new Vector3f(1.0F, 0.0F, 0.0F);
        private static final Vector3f worldPos = new Vector3f();
        private static final Vector4f t = new Vector4f();
        private static final Matrix4f m = new Matrix4f();

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            instance.getInterpolatedFacingDirection(TEMP, partialTick);
            float length = TEMP.length();
            if (length < 1.0E-4F) TEMP.zero();
            else TEMP.div(length);
            MathHelper.setFromUnitVectors(X, TEMP, quaternion);
            Vec3 pos = camera.position();
            instance.getRenderWorldPosition(worldPos, partialTick);
            t.set(
                    pos.x - worldPos.x,
                    pos.y - worldPos.y,
                    pos.z - worldPos.z,
                    0
            );
            toParticleSpace(instance, t).mul(m.rotation(quaternion).invert());
            quaternion.rotateX((float) Mth.atan2(-t.y, t.z));
        }
    },
    EMITTER_TRANSFORM_XY {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_XZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_YZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, -Mth.HALF_PI, 0.0F);
        }
    };

    public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
        setRotation(quaternion, camera, partialTick);
    }

    private static final Vector3f WORLD_UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f FALLBACK_UP = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final Vector3f AXIS_X = new Vector3f();
    private static final Vector3f AXIS_Y = new Vector3f();
    private static final Vector3f AXIS_Z = new Vector3f();
    private static final Vector3f TEMP = new Vector3f();
    private static final Matrix3f DIRECTION_MATRIX = new Matrix3f();

    private static boolean setDirectionXRotation(IMolangParticleInstance instance, Quaternionf quaternion, float partialTick) {
        if (!readDirection(instance, AXIS_X, partialTick)) {
            return false;
        }

        setUpAxis(AXIS_X, AXIS_Y);
        AXIS_Z.set(AXIS_X).cross(AXIS_Y).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean setDirectionYRotation(IMolangParticleInstance instance, Quaternionf quaternion, float partialTick) {
        if (!readDirection(instance, AXIS_Y, partialTick)) {
            return false;
        }

        setUpAxis(AXIS_Y, AXIS_X);
        AXIS_Z.set(AXIS_X).cross(AXIS_Y).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean setDirectionZRotation(IMolangParticleInstance instance, Quaternionf quaternion, float partialTick) {
        if (!readDirection(instance, AXIS_Z, partialTick)) {
            return false;
        }

        AXIS_Z.negate();
        setUpAxis(AXIS_Z, AXIS_Y);
        AXIS_X.set(AXIS_Y).cross(AXIS_Z).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean readDirection(IMolangParticleInstance instance, Vector3f dest, float partialTick) {
        instance.getInterpolatedFacingDirection(dest, partialTick);
        float length = dest.length();
        if (length < 1.0E-4F) return false;
        dest.div(length);
        return true;
    }

    private static void setUpAxis(Vector3f lockedAxis, Vector3f dest) {
        Vector3f up = java.lang.Math.abs(WORLD_UP.dot(lockedAxis)) > 0.999F ? FALLBACK_UP : WORLD_UP;
        dest.set(up).sub(TEMP.set(lockedAxis).mul(up.dot(lockedAxis))).normalize();
    }

    private static void setFromAxes(Quaternionf quaternion, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis) {
        quaternion.setFromNormalized(DIRECTION_MATRIX.set(
                xAxis.x, yAxis.x, zAxis.x,
                xAxis.y, yAxis.y, zAxis.y,
                xAxis.z, yAxis.z, zAxis.z
        ).invert());
    }

    private static Vector3f toParticleSpace(IMolangParticleInstance instance, Vector3f direction) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? direction : direction.rotate(new Quaternionf(rotation).conjugate());
    }

    private static Vector4f toParticleSpace(IMolangParticleInstance instance, Vector4f direction) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? direction : direction.rotate(new Quaternionf(rotation).conjugate());
    }

    private static Quaternionf toParticleSpace(IMolangParticleInstance instance, Quaternionf orientation) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? orientation : orientation.premul(new Quaternionf(rotation).conjugate());
    }

    public Matrix3f setRenderBasis(IMolangParticleInstance instance, Matrix3f matrix, Vector3f scratch,
                                   Camera camera, float partialTick) {
        switch (this) {
            case DO_NOTHING -> { return matrix.identity(); }
            case ROTATE_XYZ -> { return matrix.rotation(camera.rotation()); }
            case ROTATE_Y -> {
                Quaternionf rotation = camera.rotation();
                float x = 2.0F * (rotation.x * rotation.z + rotation.w * rotation.y);
                float z = 1.0F - 2.0F * (rotation.x * rotation.x + rotation.y * rotation.y);
                float length = (float) java.lang.Math.sqrt(x * x + z * z);
                if (length <= java.lang.Math.ulp(1.0F)) return matrix.identity();
                x /= length;
                z /= length;
                return matrix.set(z, 0.0F, -x, 0.0F, 1.0F, 0.0F, x, 0.0F, z);
            }
            case EMITTER_TRANSFORM_XY, EMITTER_TRANSFORM_XZ, EMITTER_TRANSFORM_YZ -> {
                var emitter = instance.getEmitter();
                if (emitter == null) matrix.identity();
                else emitter.getParticleRenderBasis(matrix);
                return switch (this) {
                    case EMITTER_TRANSFORM_XZ -> rotateEmitterPlane(matrix, false);
                    case EMITTER_TRANSFORM_YZ -> rotateEmitterPlane(matrix, true);
                    default -> matrix;
                };
            }
            default -> {}
        }
        instance.getRenderWorldPosition(scratch, partialTick);
        Vec3 cameraPosition = camera.position();
        float viewX = (float) cameraPosition.x - scratch.x;
        float viewY = (float) cameraPosition.y - scratch.y;
        float viewZ = (float) cameraPosition.z - scratch.z;
        if (this == LOOKAT_XYZ || this == LOOKAT_Y) {
            return lookAtBasis(matrix, viewX, this == LOOKAT_Y ? 0.0F : viewY, viewZ);
        }
        instance.getInterpolatedFacingDirection(scratch, partialTick);
        var emitter = instance.getEmitter();
        if (emitter != null) emitter.getParticleRenderTransform(matrix).transform(scratch);
        float inverse = inverseDirectionLength(scratch.x, scratch.y, scratch.z);
        float dx = scratch.x * inverse;
        float dy = scratch.y * inverse;
        float dz = scratch.z * inverse;
        if (this == LOOKAT_DIRECTION) {
            float viewLength = (float) java.lang.Math.sqrt(viewZ * viewZ + viewX * viewX + viewY * viewY);
            if (viewLength <= java.lang.Math.ulp(1.0F)) return matrix.identity();
            viewX /= viewLength;
            viewY /= viewLength;
            viewZ /= viewLength;
            float yx = viewY * dz - viewZ * dy;
            float yy = viewZ * dx - viewX * dz;
            float yz = viewX * dy - viewY * dx;
            inverse = inverseDirectionLength(yx, yy, yz);
            yx *= inverse;
            yy *= inverse;
            yz *= inverse;
            return matrix.set(dx, dy, dz, yx, yy, yz,
                    yy * dz - yz * dy, yz * dx - yx * dz, yx * dy - yy * dx);
        }
        inverse = inverseDirectionLength(dx, dy, dz);
        dx *= inverse;
        dy *= inverse;
        dz *= inverse;
        boolean positivePole = dy > 0.999F;
        boolean negativePole = dy < -0.999F;
        if (this == DIRECTION_X) {
            if (positivePole || negativePole) {
                return matrix.set(dx, dy, dz, positivePole ? 1.0F : -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            }
            inverse = inverseDirectionLength(-dz, 0.0F, dx);
            float zx = -dz * inverse;
            float zz = dx * inverse;
            return matrix.set(dx, dy, dz, -zz * dy, zz * dx - zx * dz, zx * dy, zx, 0.0F, zz);
        }
        if (this == DIRECTION_Y) {
            if (positivePole || negativePole) {
                return matrix.set(positivePole ? 1.0F : -1.0F, 0.0F, 0.0F, dx, dy, dz, 0.0F, 0.0F, 1.0F);
            }
            inverse = inverseDirectionLength(-dz, 0.0F, dx);
            float xx = -dz * inverse;
            float xz = dx * inverse;
            return matrix.set(xx, 0.0F, xz, dx, dy, dz, -xz * dy, xz * dx - xx * dz, xx * dy);
        }
        if (positivePole || negativePole) {
            return matrix.set(positivePole ? 1.0F : -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, dx, dy, dz);
        }
        inverse = inverseDirectionLength(dz, 0.0F, -dx);
        float xx = dz * inverse;
        float xz = -dx * inverse;
        return matrix.set(xx, 0.0F, xz, dy * xz, dz * xx - dx * xz, -dy * xx, dx, dy, dz);
    }

    private static Matrix3f lookAtBasis(Matrix3f matrix, float x, float y, float z) {
        float length = (float) java.lang.Math.sqrt(z * z + x * x + y * y);
        if (length <= java.lang.Math.ulp(1.0F)) return matrix.identity();
        x /= length;
        y /= length;
        z /= length;
        float inverse = inverseDirectionLength(z, 0.0F, -x);
        float xx = z * inverse;
        float xz = -x * inverse;
        return matrix.set(xx, 0.0F, xz, y * xz, z * xx - x * xz, -y * xx, x, y, z);
    }

    private static float inverseDirectionLength(float x, float y, float z) {
        float length = (float) java.lang.Math.sqrt(z * z + x * x + y * y);
        return length < 1.0E-4F ? 0.0F : 1.0F / length;
    }

    private static Matrix3f rotateEmitterPlane(Matrix3f matrix, boolean yzPlane) {
        float cosine = -4.3711388E-8F;
        float axial = 0.99999994F;
        float a = matrix.m00(), b = matrix.m01(), c = matrix.m02();
        float d = matrix.m10(), e = matrix.m11(), f = matrix.m12();
        float g = matrix.m20(), h = matrix.m21(), i = matrix.m22();
        return yzPlane
                ? matrix.set(a * cosine - g, b * cosine - h, c * cosine - i,
                        d * axial, e * axial, f * axial, a + g * cosine, b + h * cosine, c + i * cosine)
                : matrix.set(a * axial, b * axial, c * axial,
                        d * cosine - g, e * cosine - h, f * cosine - i, d + g * cosine, e + h * cosine, f + i * cosine);
    }

    public static FaceCameraMode fromComponent(ParticleAppearanceBillboard.FaceCameraMode faceCameraMode) {
        return FaceCameraMode.valueOf(faceCameraMode.name());
    }
}
