package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TriangleMeshBuilder {
    private static final float NORMAL_EPSILON = 1.0E-20F;
    private static final double TRANSFORM_EPSILON = 1.0E-12;

    private float[] positions = new float[48];
    private float[] normals = new float[48];
    private float[] uvs = new float[32];
    private int[] positionIndices = new int[48];
    private int[] normalIndices = new int[48];
    private int[] uvIndices = new int[48];
    private int[] quadCornerIndices = new int[24];
    private int[] vertexColors;
    private int[] vertexLights;
    private int positionFloats;
    private int normalFloats;
    private int uvFloats;
    private int triangleIndices;
    private int quadIndices;

    public int addPosition(float x, float y, float z) {
        return addPosition(x, y, z, 0xFFFFFFFF, 0);
    }

    /** Adds a position with packed ARGB tint and packed block/sky light. */
    public int addPosition(float x, float y, float z, int color, int light) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("position coordinates must be finite");
        }
        if (positionFloats + 3 > positions.length) {
            positions = java.util.Arrays.copyOf(positions, Math.max(positions.length * 2, positionFloats + 3));
            if (vertexColors != null) {
                int previous = vertexColors.length;
                vertexColors = java.util.Arrays.copyOf(vertexColors, positions.length / 3);
                java.util.Arrays.fill(vertexColors, previous, vertexColors.length, 0xFFFFFFFF);
                vertexLights = java.util.Arrays.copyOf(vertexLights, positions.length / 3);
            }
        }
        positions[positionFloats] = x;
        positions[positionFloats + 1] = y;
        positions[positionFloats + 2] = z;
        int index = positionFloats / 3;
        if (vertexColors == null && (color != 0xFFFFFFFF || light != 0)) {
            vertexColors = new int[positions.length / 3];
            java.util.Arrays.fill(vertexColors, 0xFFFFFFFF);
            vertexLights = new int[positions.length / 3];
        }
        if (vertexColors != null) {
            vertexColors[index] = color;
            vertexLights[index] = light;
        }
        positionFloats += 3;
        return index;
    }

    public int addNormal(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("normal coordinates must be finite");
        }
        if (normalFloats + 3 > normals.length) {
            normals = java.util.Arrays.copyOf(normals, Math.max(normals.length * 2, normalFloats + 3));
        }
        normals[normalFloats] = x;
        normals[normalFloats + 1] = y;
        normals[normalFloats + 2] = z;
        int index = normalFloats / 3;
        normalFloats += 3;
        return index;
    }

    public int addUv(float u, float v) {
        if (!Float.isFinite(u) || !Float.isFinite(v)) {
            throw new IllegalArgumentException("uv coordinates must be finite");
        }
        if (uvFloats + 2 > uvs.length) {
            uvs = java.util.Arrays.copyOf(uvs, Math.max(uvs.length * 2, uvFloats + 2));
        }
        uvs[uvFloats] = u;
        uvs[uvFloats + 1] = v;
        int index = uvFloats / 2;
        uvFloats += 2;
        return index;
    }

    public void addTriangle(int position0, int position1, int position2) {
        addTriangle(position0, position1, position2, -1, -1, -1, -1, -1, -1);
    }

    public void addTriangle(int position0, int position1, int position2,
                            int normal0, int normal1, int normal2,
                            int uv0, int uv1, int uv2) {
        checkPosition(position0);
        checkPosition(position1);
        checkPosition(position2);
        checkNormal(normal0);
        checkNormal(normal1);
        checkNormal(normal2);
        checkUv(uv0);
        checkUv(uv1);
        checkUv(uv2);
        ensureIndices(triangleIndices + 3);
        positionIndices[triangleIndices] = position0;
        positionIndices[triangleIndices + 1] = position1;
        positionIndices[triangleIndices + 2] = position2;
        normalIndices[triangleIndices] = normal0;
        normalIndices[triangleIndices + 1] = normal1;
        normalIndices[triangleIndices + 2] = normal2;
        uvIndices[triangleIndices] = uv0;
        uvIndices[triangleIndices + 1] = uv1;
        uvIndices[triangleIndices + 2] = uv2;
        triangleIndices += 3;
    }

    public void addQuad(int position0, int position1, int position2, int position3,
                        int normal0, int normal1, int normal2, int normal3,
                        int uv0, int uv1, int uv2, int uv3) {
        int firstTriangle = triangleIndices / 3;
        addTriangle(position0, position1, position2, normal0, normal1, normal2, uv0, uv1, uv2);
        addTriangle(position0, position2, position3, normal0, normal2, normal3, uv0, uv2, uv3);
        appendQuad(firstTriangle);
    }

    public void addMesh(TriangleMesh mesh) {
        addMesh(mesh, new Matrix4f());
    }

    public void addMesh(TriangleMesh mesh, Matrix4f transform) {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(transform, "transform");
        float[] matrixValues = new float[16];
        transform.get(matrixValues);
        for (float value : matrixValues) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("mesh transform must be finite");
            }
        }
        Matrix3f normalMatrix = new Matrix3f(transform);
        double determinant = normalMatrix.determinant();
        if (!Double.isFinite(determinant) || Math.abs(determinant) <= TRANSFORM_EPSILON) {
            throw new IllegalArgumentException("mesh transform must be invertible");
        }
        normalMatrix.invert().transpose();
        if (mesh.triangleCount() == 0) {
            return;
        }
        int positionBase = positionFloats / 3;
        int normalBase = normalFloats / 3;
        int uvBase = uvFloats / 2;
        Vector3f point = new Vector3f();
        for (int i = 0; i < mesh.positionCount(); i++) {
            point.set(mesh.positionX(i), mesh.positionY(i), mesh.positionZ(i));
            transform.transformPosition(point);
            addPosition(point.x(), point.y(), point.z(), mesh.positionColor(i), mesh.positionLight(i));
        }
        Vector3f normal = new Vector3f();
        for (int i = 0; i < mesh.normalCount(); i++) {
            normal.set(mesh.normalX(i), mesh.normalY(i), mesh.normalZ(i));
            normalMatrix.transform(normal);
            addNormal(normal.x(), normal.y(), normal.z());
        }
        for (int i = 0; i < mesh.uvCount(); i++) {
            addUv(mesh.u(i), mesh.v(i));
        }
        boolean reverse = determinant < 0.0;
        int triangleBase = triangleIndices / 3;
        for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
            int p0 = positionBase + mesh.positionIndex(triangle, 0);
            int p1 = positionBase + mesh.positionIndex(triangle, 1);
            int p2 = positionBase + mesh.positionIndex(triangle, 2);
            int n0 = normalBase + mesh.normalIndex(triangle, 0);
            int n1 = normalBase + mesh.normalIndex(triangle, 1);
            int n2 = normalBase + mesh.normalIndex(triangle, 2);
            int u0 = uvBase + mesh.uvIndex(triangle, 0);
            int u1 = uvBase + mesh.uvIndex(triangle, 1);
            int u2 = uvBase + mesh.uvIndex(triangle, 2);
            if (reverse) {
                addTriangle(p0, p2, p1, n0, n2, n1, u0, u2, u1);
            } else {
                addTriangle(p0, p1, p2, n0, n1, n2, u0, u1, u2);
            }
        }
        if (!reverse) {
            int cornerOffset = triangleBase * 3;
            int quadCount = mesh.quadCount();
            for (int quad = 0; quad < quadCount; quad++) {
                ensureQuadCapacity(quadIndices + 4);
                for (int corner = 0; corner < 4; corner++) {
                    quadCornerIndices[quadIndices++] = mesh.quadCornerIndex(quad, corner) + cornerOffset;
                }
            }
        }
    }

    public TriangleMesh build() {
        int[] localPositions = java.util.Arrays.copyOf(positionIndices, triangleIndices);
        int[] localNormals = java.util.Arrays.copyOf(normalIndices, triangleIndices);
        int[] localUvs = java.util.Arrays.copyOf(uvIndices, triangleIndices);
        for (int triangle = 0; triangle < triangleIndices; triangle += 3) {
            Vector3f face = null;
            boolean fallbackNeeded = false;
            for (int corner = 0; corner < 3; corner++) {
                int normalIndex = localNormals[triangle + corner];
                if (normalIndex < 0 || normalLengthSquared(normalIndex) <= NORMAL_EPSILON) {
                    fallbackNeeded = true;
                    break;
                }
            }
            if (fallbackNeeded) {
                face = faceNormal(localPositions[triangle], localPositions[triangle + 1], localPositions[triangle + 2]);
                int fallback = addNormal(face.x(), face.y(), face.z());
                for (int corner = 0; corner < 3; corner++) {
                    int normalIndex = localNormals[triangle + corner];
                    if (normalIndex < 0 || normalLengthSquared(normalIndex) <= NORMAL_EPSILON) {
                        localNormals[triangle + corner] = fallback;
                    }
                }
            }
            boolean uvNeeded = false;
            for (int corner = 0; corner < 3; corner++) {
                if (localUvs[triangle + corner] < 0) {
                    uvNeeded = true;
                    break;
                }
            }
            if (uvNeeded) {
                int fallback = addUv(0.0F, 0.0F);
                for (int corner = 0; corner < 3; corner++) {
                    if (localUvs[triangle + corner] < 0) {
                        localUvs[triangle + corner] = fallback;
                    }
                }
            }
        }
        for (int i = 0; i < normalFloats; i += 3) {
            float x = normals[i];
            float y = normals[i + 1];
            float z = normals[i + 2];
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length <= 1.0E-12F) {
                normals[i] = 0.0F;
                normals[i + 1] = 1.0F;
                normals[i + 2] = 0.0F;
            } else {
                normals[i] = x / length;
                normals[i + 1] = y / length;
                normals[i + 2] = z / length;
            }
        }
        return new TriangleMesh(
                java.util.Arrays.copyOf(positions, positionFloats),
                java.util.Arrays.copyOf(normals, normalFloats),
                java.util.Arrays.copyOf(uvs, uvFloats),
                localPositions,
                localNormals,
                localUvs,
                java.util.Arrays.copyOf(quadCornerIndices, quadIndices),
                vertexColors == null ? null : java.util.Arrays.copyOf(vertexColors, positionFloats / 3),
                vertexLights == null ? null : java.util.Arrays.copyOf(vertexLights, positionFloats / 3));
    }

    private Vector3f faceNormal(int position0, int position1, int position2) {
        int a = position0 * 3;
        int b = position1 * 3;
        int c = position2 * 3;
        float abX = positions[b] - positions[a];
        float abY = positions[b + 1] - positions[a + 1];
        float abZ = positions[b + 2] - positions[a + 2];
        float acX = positions[c] - positions[a];
        float acY = positions[c + 1] - positions[a + 1];
        float acZ = positions[c + 2] - positions[a + 2];
        Vector3f normal = new Vector3f(
                abY * acZ - abZ * acY,
                abZ * acX - abX * acZ,
                abX * acY - abY * acX);
        if (normal.lengthSquared() <= NORMAL_EPSILON) {
            normal.set(0.0F, 1.0F, 0.0F);
        } else {
            normal.normalize();
        }
        return normal;
    }

    private float normalLengthSquared(int index) {
        int offset = index * 3;
        float x = normals[offset];
        float y = normals[offset + 1];
        float z = normals[offset + 2];
        return x * x + y * y + z * z;
    }

    private void checkPosition(int index) {
        if (index < 0 || index >= positionFloats / 3) {
            throw new IllegalArgumentException("position index out of range: " + index);
        }
    }

    private void checkNormal(int index) {
        if (index < -1 || index >= normalFloats / 3) {
            throw new IllegalArgumentException("normal index out of range: " + index);
        }
    }

    private void checkUv(int index) {
        if (index < -1 || index >= uvFloats / 2) {
            throw new IllegalArgumentException("uv index out of range: " + index);
        }
    }

    private void ensureIndices(int size) {
        if (size > positionIndices.length) {
            int capacity = Math.max(positionIndices.length * 2, size);
            positionIndices = java.util.Arrays.copyOf(positionIndices, capacity);
            normalIndices = java.util.Arrays.copyOf(normalIndices, capacity);
            uvIndices = java.util.Arrays.copyOf(uvIndices, capacity);
        }
    }

    private void appendQuad(int firstTriangle) {
        int firstCorner = firstTriangle * 3;
        ensureQuadCapacity(quadIndices + 4);
        quadCornerIndices[quadIndices] = firstCorner;
        quadCornerIndices[quadIndices + 1] = firstCorner + 1;
        quadCornerIndices[quadIndices + 2] = firstCorner + 2;
        quadCornerIndices[quadIndices + 3] = (firstTriangle + 1) * 3 + 2;
        quadIndices += 4;
    }

    private void ensureQuadCapacity(int size) {
        if (size > quadCornerIndices.length) {
            quadCornerIndices = java.util.Arrays.copyOf(quadCornerIndices,
                    Math.max(quadCornerIndices.length * 2, size));
        }
    }
}
