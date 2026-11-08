package com.kltyton.bedrockruntime.client.render.vanilla;

import java.util.List;
import com.kltyton.bedrockruntime.client.geometry.bounds.PartBounds;
import com.kltyton.bedrockruntime.client.render.outline.PartOutlineRenderer;
import com.kltyton.bedrockruntime.client.selection.model.VisualPartId;
import com.kltyton.bedrockruntime.client.selection.service.BonePartSelectionClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/** Limits capture to the base entity model, using its actual rendered child and baby transforms. */
public final class VanillaModelCapture {
    private static final ThreadLocal<Context> OWNER = new ThreadLocal<>();
    private VanillaModelCapture() { }

    public static void render(Entity entity, Runnable draw) {
        Context previous = OWNER.get();
        OWNER.set(new Context(entity));
        try { draw.run(); }
        finally {
            if (previous == null) { OWNER.remove(); } else { OWNER.set(previous); }
        }
    }

    public static void record(ModelPart part, PoseStack.Pose pose, String path, List<ModelPart.Cube> cubes) {
        Context context = OWNER.get();
        if (context == null || !context.observations.add(part, path, pose.pose(), cubes)) { return; }
        record(context.entity, pose, path, cubes);
    }

    public static boolean isActive() { return OWNER.get() != null; }

    public static void record(PoseStack.Pose pose, String path, List<ModelPart.Cube> cubes) {
        Context context = OWNER.get();
        if (context == null || context.observations.matchesLast(path, pose.pose(), cubes)) { return; }
        record(context.entity, pose, path, cubes);
    }

    private static void record(Entity entity, PoseStack.Pose pose, String path, List<ModelPart.Cube> cubes) {
        for (int index = 0; index < cubes.size(); index++) {
            PartBounds bounds = PartBounds.fromVanillaCube(pose.pose(), cubes.get(index));
            if (bounds == null) { continue; }
            VisualPartId id = VisualPartId.vanilla(entity, path, index);
            if (!BonePartSelectionClient.record(id, bounds)) { continue; }
            if (BonePartSelectionClient.shouldRenderOutline(id)) { PartOutlineRenderer.submit(pose, bounds, false); }
            if (BonePartSelectionClient.isDamageFlashing(id)) { PartOutlineRenderer.submit(pose, bounds, true); }
        }
    }

    private static final class Context {
        private final Entity entity;
        private final ModelPartObservations observations = new ModelPartObservations();

        private Context(Entity entity) { this.entity = entity; }
    }
}
