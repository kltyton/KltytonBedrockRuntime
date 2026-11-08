package com.kltyton.bedrockruntime.client.render.vanilla;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Tracks observations within one entity or held-item capture scope. */
public final class ModelPartObservations {
    private final Map<ModelPart, Observation> parts = new IdentityHashMap<>();
    private Observation last;

    public boolean add(ModelPart part, String path, Matrix4fc pose, List<ModelPart.Cube> cubes) {
        Observation previous = parts.get(part);
        if (matches(previous, path, pose, cubes)) {
            last = previous;
            return false;
        }
        last = new Observation(path, new Matrix4f(pose), List.copyOf(cubes));
        parts.put(part, last);
        return true;
    }

    public boolean matchesLast(String path, Matrix4fc pose, List<ModelPart.Cube> cubes) {
        return matches(last, path, pose, cubes);
    }

    private static boolean matches(Observation previous, String path, Matrix4fc pose, List<ModelPart.Cube> cubes) {
        return previous != null && previous.path.equals(path) && previous.pose.equals(pose) && previous.cubes.equals(cubes);
    }

    private record Observation(String path, Matrix4f pose, List<ModelPart.Cube> cubes) { }
}
