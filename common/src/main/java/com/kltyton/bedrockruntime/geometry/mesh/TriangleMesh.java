package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TriangleMesh {
    private static final double RAY_EPSILON = 1.0E-12;

    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final int[] positionIndices;
    private final int[] normalIndices;
    private final int[] uvIndices;
    private final int[] quadCornerIndices;
    private final int[] vertexColors;
    private final int[] vertexLights;
    private final int triangleCount;
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    TriangleMesh(float[] positions, float[] normals, float[] uvs,
                 int[] positionIndices, int[] normalIndices, int[] uvIndices) {
        this(positions, normals, uvs, positionIndices, normalIndices, uvIndices, new int[0]);
    }

    TriangleMesh(float[] positions, float[] normals, float[] uvs,
                  int[] positionIndices, int[] normalIndices, int[] uvIndices, int[] quadCornerIndices) {
        this(positions, normals, uvs, positionIndices, normalIndices, uvIndices, quadCornerIndices, null, null);
    }

    TriangleMesh(float[] positions, float[] normals, float[] uvs,
                 int[] positionIndices, int[] normalIndices, int[] uvIndices, int[] quadCornerIndices,
                 int[] vertexColors, int[] vertexLights) {
        this.positions = Objects.requireNonNull(positions, "positions").clone();
        this.normals = Objects.requireNonNull(normals, "normals").clone();
        this.uvs = Objects.requireNonNull(uvs, "uvs").clone();
        this.positionIndices = Objects.requireNonNull(positionIndices, "positionIndices").clone();
        this.normalIndices = Objects.requireNonNull(normalIndices, "normalIndices").clone();
        this.uvIndices = Objects.requireNonNull(uvIndices, "uvIndices").clone();
        this.quadCornerIndices = Objects.requireNonNull(quadCornerIndices, "quadCornerIndices").clone();
        this.vertexColors = vertexColors == null ? null : vertexColors.clone();
        this.vertexLights = vertexLights == null ? null : vertexLights.clone();
        if ((this.vertexColors != null && this.vertexColors.length != this.positions.length / 3)
                || (this.vertexLights != null && this.vertexLights.length != this.positions.length / 3)) {
            throw new IllegalArgumentException("vertex color/light counts must match position count");
        }
        if ((this.positionIndices.length % 3) != 0) {
            throw new IllegalArgumentException("position index count must be a multiple of three");
        }
        if (this.normalIndices.length != this.positionIndices.length
                || this.uvIndices.length != this.positionIndices.length) {
            throw new IllegalArgumentException("triangle attribute index counts must match");
        }
        if ((this.positions.length % 3) != 0 || (this.normals.length % 3) != 0 || (this.uvs.length & 1) != 0) {
            throw new IllegalArgumentException("vertex array sizes must be complete");
        }
        this.triangleCount = this.positionIndices.length / 3;
        int positionCount = this.positions.length / 3;
        int normalCount = this.normals.length / 3;
        int uvCount = this.uvs.length / 2;
        for (int i = 0; i < this.positions.length; i++) {
            if (!Float.isFinite(this.positions[i])) {
                throw new IllegalArgumentException("position coordinates must be finite");
            }
        }
        for (int i = 0; i < this.normals.length; i++) {
            if (!Float.isFinite(this.normals[i])) {
                throw new IllegalArgumentException("normal coordinates must be finite");
            }
        }
        for (int i = 0; i < this.uvs.length; i++) {
            if (!Float.isFinite(this.uvs[i])) {
                throw new IllegalArgumentException("uv coordinates must be finite");
            }
        }
        for (int i = 0; i < this.positionIndices.length; i++) {
            if (this.positionIndices[i] < 0 || this.positionIndices[i] >= positionCount) {
                throw new IllegalArgumentException("position index out of range");
            }
            if (this.normalIndices[i] < 0 || this.normalIndices[i] >= normalCount) {
                throw new IllegalArgumentException("normal index out of range");
            }
            if (this.uvIndices[i] < 0 || this.uvIndices[i] >= uvCount) {
                throw new IllegalArgumentException("uv index out of range");
            }
        }
        if ((this.quadCornerIndices.length % 4) != 0) {
            throw new IllegalArgumentException("quad corner count must be a multiple of four");
        }
        int previousQuadFirstTriangle = -2;
        for (int quad = 0; quad < quadCount(); quad++) {
            int base = quad * 4;
            int firstCorner = this.quadCornerIndices[base];
            if ((firstCorner % 3) != 0) {
                throw new IllegalArgumentException("quad must begin at a triangle's first corner");
            }
            int firstTriangle = firstCorner / 3;
            if (firstTriangle < 0 || firstTriangle + 1 >= this.triangleCount) {
                throw new IllegalArgumentException("quad triangle range is invalid");
            }
            if (firstTriangle <= previousQuadFirstTriangle + 1) {
                throw new IllegalArgumentException("quad triangles must be distinct and ordered");
            }
            if (this.quadCornerIndices[base + 1] != firstCorner + 1
                    || this.quadCornerIndices[base + 2] != firstCorner + 2
                    || this.quadCornerIndices[base + 3] != (firstTriangle + 1) * 3 + 2) {
                throw new IllegalArgumentException("quad corner provenance is invalid");
            }
            previousQuadFirstTriangle = firstTriangle;
        }
        if (this.triangleCount == 0) {
            this.minX = 0.0F;
            this.minY = 0.0F;
            this.minZ = 0.0F;
            this.maxX = 0.0F;
            this.maxY = 0.0F;
            this.maxZ = 0.0F;
        } else {
            float lowX = Float.POSITIVE_INFINITY;
            float lowY = Float.POSITIVE_INFINITY;
            float lowZ = Float.POSITIVE_INFINITY;
            float highX = Float.NEGATIVE_INFINITY;
            float highY = Float.NEGATIVE_INFINITY;
            float highZ = Float.NEGATIVE_INFINITY;
            for (int index : this.positionIndices) {
                int offset = index * 3;
                float x = this.positions[offset];
                float y = this.positions[offset + 1];
                float z = this.positions[offset + 2];
                lowX = Math.min(lowX, x);
                lowY = Math.min(lowY, y);
                lowZ = Math.min(lowZ, z);
                highX = Math.max(highX, x);
                highY = Math.max(highY, y);
                highZ = Math.max(highZ, z);
            }
            this.minX = lowX;
            this.minY = lowY;
            this.minZ = lowZ;
            this.maxX = highX;
            this.maxY = highY;
            this.maxZ = highZ;
        }
    }

    public int triangleCount() {
        return triangleCount;
    }

    public int quadCount() {
        return quadCornerIndices.length / 4;
    }

    public int quadCornerIndex(int quad, int corner) {
        if (quad < 0 || quad >= quadCount()) {
            throw new IndexOutOfBoundsException("quad " + quad);
        }
        if (corner < 0 || corner > 3) {
            throw new IndexOutOfBoundsException("corner " + corner);
        }
        return quadCornerIndices[quad * 4 + corner];
    }

    public int positionCount() {
        return positions.length / 3;
    }

    public int normalCount() {
        return normals.length / 3;
    }

    public int uvCount() {
        return uvs.length / 2;
    }

    public float minX() {
        return minX;
    }

    public float minY() {
        return minY;
    }

    public float minZ() {
        return minZ;
    }

    public float maxX() {
        return maxX;
    }

    public float maxY() {
        return maxY;
    }

    public float maxZ() {
        return maxZ;
    }

    public float positionX(int positionIndex) {
        check(positionIndex, positionCount(), "position");
        return positions[positionIndex * 3];
    }

    public float positionY(int positionIndex) {
        check(positionIndex, positionCount(), "position");
        return positions[positionIndex * 3 + 1];
    }

    public float positionZ(int positionIndex) {
        check(positionIndex, positionCount(), "position");
        return positions[positionIndex * 3 + 2];
    }

    /** Packed ARGB model tint, with opaque white as the absent-channel value. */
    public int positionColor(int positionIndex) {
        check(positionIndex, positionCount(), "position");
        return vertexColors == null ? 0xFFFFFFFF : vertexColors[positionIndex];
    }

    /** Packed block/sky light contributed by the baked model. */
    public int positionLight(int positionIndex) {
        check(positionIndex, positionCount(), "position");
        return vertexLights == null ? 0 : vertexLights[positionIndex];
    }

    public int color(int triangle, int corner) {
        int position = positionIndex(triangle, corner);
        return vertexColors == null ? 0xFFFFFFFF : vertexColors[position];
    }

    public int light(int triangle, int corner) {
        int position = positionIndex(triangle, corner);
        return vertexLights == null ? 0 : vertexLights[position];
    }


    public float normalX(int normalIndex) {
        check(normalIndex, normalCount(), "normal");
        return normals[normalIndex * 3];
    }

    public float normalY(int normalIndex) {
        check(normalIndex, normalCount(), "normal");
        return normals[normalIndex * 3 + 1];
    }

    public float normalZ(int normalIndex) {
        check(normalIndex, normalCount(), "normal");
        return normals[normalIndex * 3 + 2];
    }

    public float u(int uvIndex) {
        check(uvIndex, uvCount(), "uv");
        return uvs[uvIndex * 2];
    }

    public float v(int uvIndex) {
        check(uvIndex, uvCount(), "uv");
        return uvs[uvIndex * 2 + 1];
    }

    public int positionIndex(int triangle, int corner) {
        return positionIndices[index(triangle, corner)];
    }

    public int normalIndex(int triangle, int corner) {
        return normalIndices[index(triangle, corner)];
    }

    public int uvIndex(int triangle, int corner) {
        return uvIndices[index(triangle, corner)];
    }

    public float x(int triangle, int corner) {
        return positionX(positionIndex(triangle, corner));
    }

    public float y(int triangle, int corner) {
        return positionY(positionIndex(triangle, corner));
    }

    public float z(int triangle, int corner) {
        return positionZ(positionIndex(triangle, corner));
    }

    public float u(int triangle, int corner) {
        return u(uvIndex(triangle, corner));
    }

    public float v(int triangle, int corner) {
        return v(uvIndex(triangle, corner));
    }

    public float nx(int triangle, int corner) {
        return normalX(normalIndex(triangle, corner));
    }

    public float ny(int triangle, int corner) {
        return normalY(normalIndex(triangle, corner));
    }

    public float nz(int triangle, int corner) {
        return normalZ(normalIndex(triangle, corner));
    }

    public float[] copyPositions() {
        return positions.clone();
    }

    public float[] copyNormals() {
        return normals.clone();
    }

    public float[] copyUvs() {
        return uvs.clone();
    }

    public int[] copyPositionIndices() {
        return positionIndices.clone();
    }

    public int[] copyNormalIndices() {
        return normalIndices.clone();
    }

    public int[] copyUvIndices() {
        return uvIndices.clone();
    }

    public double rayIntersection(double originX, double originY, double originZ,
                                  double directionX, double directionY, double directionZ,
                                  double maxDistance, boolean doubleSided) {
        if (!Double.isFinite(originX) || !Double.isFinite(originY) || !Double.isFinite(originZ)
                || !Double.isFinite(directionX) || !Double.isFinite(directionY) || !Double.isFinite(directionZ)
                || Double.isNaN(maxDistance) || maxDistance < 0.0 || triangleCount == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        if (lengthSquared <= RAY_EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        double length = Math.sqrt(lengthSquared);
        double maxTime = Double.isInfinite(maxDistance) ? Double.POSITIVE_INFINITY : maxDistance / length;
        double[] bounds = { 0.0, maxTime };
        if (!clipAxis(originX, directionX, minX, maxX, bounds)
                || !clipAxis(originY, directionY, minY, maxY, bounds)
                || !clipAxis(originZ, directionZ, minZ, maxZ, bounds)) {
            return Double.POSITIVE_INFINITY;
        }
        double nearest = Double.POSITIVE_INFINITY;
        for (int triangle = 0; triangle < triangleCount; triangle++) {
            int offset = triangle * 3;
            int a = positionIndices[offset];
            int b = positionIndices[offset + 1];
            int c = positionIndices[offset + 2];
            double hit = intersectTriangle(originX, originY, originZ, directionX, directionY, directionZ,
                    a, b, c, doubleSided);
            if (hit >= 0.0 && hit <= maxTime + RAY_EPSILON && hit < nearest) {
                nearest = hit;
            }
        }
        return nearest == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : nearest * length;
    }

    public TriangleMesh transformed(Matrix4f transform) {
        Objects.requireNonNull(transform, "transform");
        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        builder.addMesh(this, transform);
        return builder.build();
    }

    private double intersectTriangle(double originX, double originY, double originZ,
                                     double directionX, double directionY, double directionZ,
                                     int a, int b, int c, boolean doubleSided) {
        int ao = a * 3;
        int bo = b * 3;
        int co = c * 3;
        double ax = positions[ao];
        double ay = positions[ao + 1];
        double az = positions[ao + 2];
        double edge1X = positions[bo] - ax;
        double edge1Y = positions[bo + 1] - ay;
        double edge1Z = positions[bo + 2] - az;
        double edge2X = positions[co] - ax;
        double edge2Y = positions[co + 1] - ay;
        double edge2Z = positions[co + 2] - az;
        double pX = directionY * edge2Z - directionZ * edge2Y;
        double pY = directionZ * edge2X - directionX * edge2Z;
        double pZ = directionX * edge2Y - directionY * edge2X;
        double determinant = edge1X * pX + edge1Y * pY + edge1Z * pZ;
        if (doubleSided) {
            if (Math.abs(determinant) <= RAY_EPSILON) {
                return -1.0;
            }
        } else if (determinant <= RAY_EPSILON) {
            return -1.0;
        }
        double inverse = 1.0 / determinant;
        double tX = originX - ax;
        double tY = originY - ay;
        double tZ = originZ - az;
        double u = (tX * pX + tY * pY + tZ * pZ) * inverse;
        if (u < -1.0E-9 || u > 1.0 + 1.0E-9) {
            return -1.0;
        }
        double qX = tY * edge1Z - tZ * edge1Y;
        double qY = tZ * edge1X - tX * edge1Z;
        double qZ = tX * edge1Y - tY * edge1X;
        double v = (directionX * qX + directionY * qY + directionZ * qZ) * inverse;
        if (v < -1.0E-9 || u + v > 1.0 + 1.0E-9) {
            return -1.0;
        }
        double time = (edge2X * qX + edge2Y * qY + edge2Z * qZ) * inverse;
        return time >= -1.0E-9 ? time : -1.0;
    }

    private static boolean clipAxis(double origin, double direction, double minimum, double maximum, double[] range) {
        if (Math.abs(direction) <= RAY_EPSILON) {
            return origin >= minimum && origin <= maximum;
        }
        double first = (minimum - origin) / direction;
        double second = (maximum - origin) / direction;
        if (first > second) {
            double swap = first;
            first = second;
            second = swap;
        }
        range[0] = Math.max(range[0], first);
        range[1] = Math.min(range[1], second);
        return range[0] <= range[1];
    }

    private int index(int triangle, int corner) {
        if (triangle < 0 || triangle >= triangleCount) {
            throw new IndexOutOfBoundsException("triangle " + triangle);
        }
        if (corner < 0 || corner > 2) {
            throw new IndexOutOfBoundsException("corner " + corner);
        }
        return triangle * 3 + corner;
    }

    private static void check(int index, int count, String kind) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(kind + " " + index);
        }
    }
}
