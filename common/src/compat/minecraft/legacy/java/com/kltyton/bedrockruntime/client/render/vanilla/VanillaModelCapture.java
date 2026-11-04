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
    private static final ThreadLocal<Entity> OWNER = new ThreadLocal<>();
    private VanillaModelCapture() { }

    public static void render(Entity entity, Runnable draw) {
        Entity previous = OWNER.get();
        OWNER.set(entity);
        try { draw.run(); }
        finally {
            if (previous == null) { OWNER.remove(); } else { OWNER.set(previous); }
        }
    }

    public static void record(PoseStack.Pose pose, String path, List<ModelPart.Cube> cubes) {
        Entity entity = OWNER.get();
        if (entity == null) { return; }
        for (int index = 0; index < cubes.size(); index++) {
            PartBounds bounds = PartBounds.fromVanillaCube(pose.pose(), cubes.get(index));
            if (bounds == null) { continue; }
            VisualPartId id = VisualPartId.vanilla(entity, path, index);
            if (!BonePartSelectionClient.record(id, bounds)) { continue; }
            if (BonePartSelectionClient.shouldRenderOutline(id)) { PartOutlineRenderer.submit(pose, bounds, false); }
            if (BonePartSelectionClient.isDamageFlashing(id)) { PartOutlineRenderer.submit(pose, bounds, true); }
        }
    }
}
