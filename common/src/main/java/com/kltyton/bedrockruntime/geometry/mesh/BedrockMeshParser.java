package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.joml.Vector3d;
import org.joml.Vector3f;

public final class BedrockMeshParser {
    private static final double POSITION_SCALE = 1.0 / 16.0;
    private static final double NORMAL_EPSILON_SQUARED = 1.0E-20;

    private BedrockMeshParser() {
    }

    public static TriangleMesh parse(String resource, String bone, JsonObject polyMesh,
                                     float textureWidth, float textureHeight) {
        String safeResource = Objects.toString(resource, "");
        String safeBone = Objects.toString(bone, "");
        try {
            return parseInternal(safeResource, safeBone, polyMesh, textureWidth, textureHeight);
        } catch (MeshParseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MeshParseException(safeResource, safeBone,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                    exception);
        }
    }

    private static TriangleMesh parseInternal(String resource, String bone, JsonObject polyMesh,
                                              float textureWidth, float textureHeight) {
        if (polyMesh == null) {
            throw fail(resource, bone, "poly_mesh object is missing");
        }
        boolean normalizedUvs = booleanValue(polyMesh.get("normalized_uvs"), false);
        List<Vector3f> positions = parsePositions(resource, bone, polyMesh.get("positions"));
        List<Vector3f> normals = parseNormals(resource, bone, polyMesh.get("normals"));
        List<float[]> uvs = parseUvs(resource, bone, polyMesh.get("uvs"), normalizedUvs, textureWidth, textureHeight);
        JsonElement polys = polyMesh.get("polys");
        if (polys == null || polys.isJsonNull()) {
            throw fail(resource, bone, "polys is missing");
        }

        TriangleMeshBuilder builder = new TriangleMeshBuilder();
        for (Vector3f position : positions) {
            builder.addPosition(position.x(), position.y(), position.z());
        }
        for (Vector3f normal : normals) {
            builder.addNormal(normal.x(), normal.y(), normal.z());
        }
        for (float[] uv : uvs) {
            builder.addUv(uv[0], uv[1]);
        }
        if (polys.isJsonPrimitive()) {
            if (!polys.getAsJsonPrimitive().isString()) {
                throw fail(resource, bone, "polys primitive must be a string mode");
            }
            String mode = polys.getAsString();
            if ("tri_list".equals(mode)) {
                parseListMode(resource, bone, positions, normals, uvs, builder, 3);
            } else if ("quad_list".equals(mode)) {
                parseListMode(resource, bone, positions, normals, uvs, builder, 4);
            } else {
                throw fail(resource, bone, "unsupported polys mode: " + mode);
            }
        } else if (polys.isJsonArray()) {
            JsonArray polygons = polys.getAsJsonArray();
            if (polygons.size() == 0) {
                throw fail(resource, bone, "polys array is empty");
            }
            for (int i = 0; i < polygons.size(); i++) {
                JsonElement element = polygons.get(i);
                if (element == null || !element.isJsonArray()) {
                    throw fail(resource, bone, "polygon " + i + " must be an array");
                }
                parseIndexedPolygon(resource, bone, positions, normals, uvs, element.getAsJsonArray(),
                        builder, "polygon " + i);
            }
        } else {
            throw fail(resource, bone, "polys must be a mode string or polygon array");
        }

        TriangleMesh mesh = builder.build();
        if (mesh.triangleCount() == 0) {
            throw fail(resource, bone, "poly_mesh produced no triangles");
        }
        return mesh;
    }

    private static List<Vector3f> parsePositions(String resource, String bone, JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            throw fail(resource, bone, "positions must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() == 0) {
            throw fail(resource, bone, "positions is empty");
        }
        List<Vector3f> positions = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonArray point = arrayAt(resource, bone, array.get(i), "positions[" + i + "]");
            if (point.size() < 3) {
                throw fail(resource, bone, "positions[" + i + "] must contain x, y and z");
            }
            double x = number(resource, bone, point.get(0), "positions[" + i + "][0]");
            double y = number(resource, bone, point.get(1), "positions[" + i + "][1]");
            double z = number(resource, bone, point.get(2), "positions[" + i + "][2]");
            positions.add(new Vector3f(
                    finiteFloat(resource, bone, -x * POSITION_SCALE, "positions[" + i + "] x"),
                    finiteFloat(resource, bone, y * POSITION_SCALE, "positions[" + i + "] y"),
                    finiteFloat(resource, bone, z * POSITION_SCALE, "positions[" + i + "] z")));
        }
        return positions;
    }

    private static List<Vector3f> parseNormals(String resource, String bone, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw fail(resource, bone, "normals must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        List<Vector3f> normals = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonArray normal = arrayAt(resource, bone, array.get(i), "normals[" + i + "]");
            if (normal.size() < 3) {
                throw fail(resource, bone, "normals[" + i + "] must contain x, y and z");
            }
            float x = finiteFloat(resource, bone,
                    -number(resource, bone, normal.get(0), "normals[" + i + "][0]"), "normals[" + i + "] x");
            float y = finiteFloat(resource, bone,
                    number(resource, bone, normal.get(1), "normals[" + i + "][1]"), "normals[" + i + "] y");
            float z = finiteFloat(resource, bone,
                    number(resource, bone, normal.get(2), "normals[" + i + "][2]"), "normals[" + i + "] z");
            Vector3f converted = new Vector3f(x, y, z);
            if (converted.lengthSquared() > NORMAL_EPSILON_SQUARED) {
                converted.normalize();
            } else {
                converted.zero();
            }
            normals.add(converted);
        }
        return normals;
    }

    private static List<float[]> parseUvs(String resource, String bone, JsonElement element, boolean normalized,
                                          float textureWidth, float textureHeight) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw fail(resource, bone, "uvs must be an array");
        }
        if (!normalized && (!Float.isFinite(textureWidth) || !Float.isFinite(textureHeight)
                || textureWidth <= 0.0F || textureHeight <= 0.0F)) {
            throw fail(resource, bone, "texture width and height must be positive and finite for non-normalized uvs");
        }
        JsonArray array = element.getAsJsonArray();
        List<float[]> uvs = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonArray uv = arrayAt(resource, bone, array.get(i), "uvs[" + i + "]");
            if (uv.size() < 2) {
                throw fail(resource, bone, "uvs[" + i + "] must contain u and v");
            }
            double u = number(resource, bone, uv.get(0), "uvs[" + i + "][0]");
            double v = number(resource, bone, uv.get(1), "uvs[" + i + "][1]");
            if (!normalized) {
                u /= textureWidth;
                v /= textureHeight;
            }
            v = 1.0 - v;
            uvs.add(new float[]{
                    finiteFloat(resource, bone, u, "uvs[" + i + "] u"),
                    finiteFloat(resource, bone, v, "uvs[" + i + "] v")});
        }
        return uvs;
    }

    private static void parseListMode(String resource, String bone, List<Vector3f> positions,
                                      List<Vector3f> normals, List<float[]> uvs, TriangleMeshBuilder builder,
                                      int stride) {
        if (positions.size() % stride != 0) {
            throw fail(resource, bone, "positions count must be a multiple of " + stride + " for this polys mode");
        }
        if (!normals.isEmpty() && normals.size() < positions.size()) {
            throw fail(resource, bone, "normals count is smaller than positions count");
        }
        if (!uvs.isEmpty() && uvs.size() < positions.size()) {
            throw fail(resource, bone, "uvs count is smaller than positions count");
        }
        for (int start = 0; start < positions.size(); start += stride) {
            List<VertexRef> polygon = new ArrayList<>(stride);
            for (int corner = 0; corner < stride; corner++) {
                int index = start + corner;
                polygon.add(new VertexRef(index,
                        normals.isEmpty() ? -1 : index,
                        uvs.isEmpty() ? -1 : index));
            }
            triangulatePolygon(resource, bone, positions, normals, uvs, polygon, builder,
                    "polygon " + (start / stride));
        }
    }

    private static void parseIndexedPolygon(String resource, String bone, List<Vector3f> positions,
                                            List<Vector3f> normals, List<float[]> uvs, JsonArray polygon,
                                            TriangleMeshBuilder builder, String label) {
        if (polygon.size() < 3) {
            throw fail(resource, bone, label + " must contain at least three vertices");
        }
        List<VertexRef> vertices = new ArrayList<>(polygon.size());
        for (int i = 0; i < polygon.size(); i++) {
            JsonArray vertex = arrayAt(resource, bone, polygon.get(i), label + " vertex " + i);
            if (vertex.size() < 1 || vertex.size() > 3) {
                throw fail(resource, bone, label + " vertex " + i + " must contain 1..3 indices");
            }
            int position = index(resource, bone, vertex.get(0), label + " vertex " + i + " position");
            if (position < 0 || position >= positions.size()) {
                throw fail(resource, bone, label + " position index out of range: " + position);
            }
            int normal;
            if (vertex.size() > 1) {
                normal = index(resource, bone, vertex.get(1), label + " vertex " + i + " normal");
                if (normals.isEmpty()) {
                    throw fail(resource, bone, label + " references normals but normals is missing");
                }
                if (normal < 0 || normal >= normals.size()) {
                    throw fail(resource, bone, label + " normal index out of range: " + normal);
                }
            } else {
                normal = normals.isEmpty() ? -1 : position;
                if (normal >= normals.size()) {
                    throw fail(resource, bone, label + " implicit normal index out of range: " + normal);
                }
            }
            int uv;
            if (vertex.size() > 2) {
                uv = index(resource, bone, vertex.get(2), label + " vertex " + i + " uv");
                if (uvs.isEmpty()) {
                    throw fail(resource, bone, label + " references uvs but uvs is missing");
                }
                if (uv < 0 || uv >= uvs.size()) {
                    throw fail(resource, bone, label + " uv index out of range: " + uv);
                }
            } else {
                uv = uvs.isEmpty() ? -1 : position;
                if (uv >= uvs.size()) {
                    throw fail(resource, bone, label + " implicit uv index out of range: " + uv);
                }
            }
            vertices.add(new VertexRef(position, normal, uv));
        }
        if (vertices.size() > 1 && vertices.get(0).positionIndex == vertices.get(vertices.size() - 1).positionIndex) {
            vertices.remove(vertices.size() - 1);
        }
        if (vertices.size() < 3) {
            throw fail(resource, bone, label + " has fewer than three distinct vertices");
        }
        triangulatePolygon(resource, bone, positions, normals, uvs, vertices, builder, label);
    }

    private static void triangulatePolygon(String resource, String bone, List<Vector3f> positions,
                                           List<Vector3f> normals, List<float[]> uvs, List<VertexRef> vertices,
                                           TriangleMeshBuilder builder, String label) {
        List<Vector3d> points = new ArrayList<>(vertices.size());
        for (VertexRef vertex : vertices) {
            Vector3f position = positions.get(vertex.positionIndex);
            points.add(new Vector3d(position.x(), position.y(), position.z()));
        }
        int[][] triangles;
        try {
            triangles = MeshPolygonTriangulator.triangulate(points);
        } catch (IllegalArgumentException exception) {
            throw fail(resource, bone, label + ": " + exception.getMessage());
        }
        for (int[] triangle : triangles) {
            VertexRef a = vertices.get(triangle[0]);
            VertexRef b = vertices.get(triangle[1]);
            VertexRef c = vertices.get(triangle[2]);
            Vector3f face = faceNormal(positions.get(a.positionIndex), positions.get(b.positionIndex),
                    positions.get(c.positionIndex));
            if (face.lengthSquared() <= NORMAL_EPSILON_SQUARED) {
                throw fail(resource, bone, label + " contains a degenerate triangle");
            }
            Vector3f authored = averageNormal(normals, a, b, c);
            if (authored != null && authored.dot(face) < -1.0E-6F) {
                VertexRef swap = b;
                b = c;
                c = swap;
            }
            builder.addTriangle(
                    a.positionIndex, b.positionIndex, c.positionIndex,
                    a.normalIndex, b.normalIndex, c.normalIndex,
                    a.uvIndex, b.uvIndex, c.uvIndex);
        }
    }

    private static Vector3f averageNormal(List<Vector3f> normals, VertexRef a, VertexRef b, VertexRef c) {
        Vector3f average = new Vector3f();
        boolean found = false;
        for (VertexRef vertex : new VertexRef[]{ a, b, c }) {
            if (vertex.normalIndex < 0 || vertex.normalIndex >= normals.size()) {
                continue;
            }
            Vector3f normal = normals.get(vertex.normalIndex);
            if (normal.lengthSquared() > NORMAL_EPSILON_SQUARED) {
                average.add(normal);
                found = true;
            }
        }
        if (!found || average.lengthSquared() <= NORMAL_EPSILON_SQUARED) {
            return null;
        }
        return average.normalize();
    }

    private static Vector3f faceNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f first = new Vector3f(b).sub(a);
        Vector3f second = new Vector3f(c).sub(a);
        return first.cross(second);
    }

    private static JsonArray arrayAt(String resource, String bone, JsonElement element, String label) {
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            throw fail(resource, bone, label + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static double number(String resource, String bone, JsonElement element, String label) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            throw fail(resource, bone, label + " must be a number");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) {
            throw fail(resource, bone, label + " must be finite");
        }
        return value;
    }

    private static int index(String resource, String bone, JsonElement element, String label) {
        double value = number(resource, bone, element, label);
        if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw fail(resource, bone, label + " must be an integer");
        }
        return (int) value;
    }

    private static float finiteFloat(String resource, String bone, double value, String label) {
        float converted = (float) value;
        if (!Float.isFinite(converted)) {
            throw fail(resource, bone, label + " exceeds float range");
        }
        return converted;
    }

    private static boolean booleanValue(JsonElement element, boolean fallback) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static MeshParseException fail(String resource, String bone, String detail) {
        return new MeshParseException(resource, bone, detail);
    }

    private static final class VertexRef {
        private final int positionIndex;
        private final int normalIndex;
        private final int uvIndex;

        private VertexRef(int positionIndex, int normalIndex, int uvIndex) {
            this.positionIndex = positionIndex;
            this.normalIndex = normalIndex;
            this.uvIndex = uvIndex;
        }
    }
}
