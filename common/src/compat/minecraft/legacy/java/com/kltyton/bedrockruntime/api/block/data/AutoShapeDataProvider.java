package com.kltyton.bedrockruntime.api.block.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.bedrockruntime.api.block.shape.AutoPartShapeBlock;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/** Generates the four origin models and invisible linked-cell multipart states for any consuming mod. */
public final class AutoShapeDataProvider implements DataProvider {
    private final PackOutput output;
    private final List<? extends AutoPartShapeBlock> blocks;

    public AutoShapeDataProvider(PackOutput output, Collection<? extends AutoPartShapeBlock> blocks) {
        this.output = output;
        this.blocks = List.copyOf(blocks);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var states = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        var items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
        java.util.ArrayList<CompletableFuture<?>> tasks = new java.util.ArrayList<>();
        for (AutoPartShapeBlock block : blocks) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            JsonObject state = new JsonObject();
            JsonArray multipart = new JsonArray();
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                JsonObject when = new JsonObject();
                when.addProperty("facing", facing.getSerializedName());
                when.addProperty("origin_x", Integer.toString(AutoPartShapeBlock.MAX_PART_OFFSET));
                when.addProperty("origin_y", Integer.toString(AutoPartShapeBlock.MAX_PART_OFFSET));
                when.addProperty("origin_z", Integer.toString(AutoPartShapeBlock.MAX_PART_OFFSET));
                JsonObject apply = new JsonObject();
                apply.addProperty("model", id.withPrefix("block/").toString());
                apply.addProperty("y", switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; });
                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", apply);
                multipart.add(part);
            }
            state.add("multipart", multipart);
            tasks.add(DataProvider.saveStable(cache, state, states.json(id)));
            if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                JsonObject item = new JsonObject();
                item.addProperty("parent", id.withPrefix("block/").toString());
                tasks.add(DataProvider.saveStable(cache, item, items.json(BuiltInRegistries.ITEM.getKey(block.asItem()))));
            }
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() { return "Automatic model block states and items"; }
}
