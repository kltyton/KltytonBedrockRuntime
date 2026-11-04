package com.kltyton.bedrockruntime.client.particle.model.api;

import com.kltyton.bedrockruntime.geometry.mesh.TriangleMesh;
import org.joml.Matrix4f;

import java.util.Objects;

/**
 * One immutable drawable section of a model particle.
 *
 * @param texture sprite identifier in the particle atlas; empty means "use the model texture"
 * @param material material pass for this section
 * @param uvSpace UV convention of {@code mesh}
 * @param mesh immutable triangle/quad mesh
 * @param transform additional model-space transform applied before the particle pose
 * @param doubleSided whether the runtime may emit a second, opposite-winding face
 */
public record ModelParticlePart(String texture, ModelMaterial material, ModelUvSpace uvSpace,
                                TriangleMesh mesh, Matrix4f transform, boolean doubleSided) {
    public ModelParticlePart {
        texture = texture == null ? "" : texture.trim();
        material = Objects.requireNonNull(material, "material");
        uvSpace = Objects.requireNonNull(uvSpace, "uvSpace");
        mesh = Objects.requireNonNull(mesh, "mesh");
        transform = new Matrix4f(Objects.requireNonNull(transform, "transform"));
        if (!finite(transform)) {
            throw new IllegalArgumentException("model particle transform must be finite");
        }
    }

    public ModelParticlePart(String texture, ModelMaterial material, ModelUvSpace uvSpace,
                             TriangleMesh mesh, Matrix4f transform) {
        this(texture, material, uvSpace, mesh, transform, false);
    }

    public ModelParticlePart(String texture, TriangleMesh mesh, Matrix4f transform) {
        this(texture, ModelMaterial.SOLID, ModelUvSpace.NORMALIZED, mesh, transform);
    }

    @Override
    public Matrix4f transform() {
        return new Matrix4f(transform);
    }

    /** Post-multiplies the caller-owned particle pose by this section's local transform. */
    public Matrix4f appendTransform(Matrix4f destination) {
        return destination.mul(transform);
    }

    private static boolean finite(Matrix4f matrix) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                if (!Float.isFinite(matrix.get(column, row))) {
                    return false;
                }
            }
        }
        return true;
    }
}
