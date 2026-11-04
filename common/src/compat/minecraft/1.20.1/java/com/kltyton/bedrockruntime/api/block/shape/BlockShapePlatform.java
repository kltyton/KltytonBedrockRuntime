package com.kltyton.bedrockruntime.api.block.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.kltyton.bedrockruntime.config.common.BoneHitboxConfig;

/** Native id/registry/offset adapters shared by the 1.20.1 block-shape algorithm callers. */
final class BlockShapePlatform {
    private BlockShapePlatform() { }

    static String canonicalId(String id) {
        return new ResourceLocation(id).toString();
    }

    static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    static Block blockById(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
    }

    static Iterable<Block> blocks() {
        return BuiltInRegistries.BLOCK;
    }

    static boolean allowsBlockModel(Block block) {
        return BoneHitboxConfig.allowsBlockModel(BuiltInRegistries.BLOCK.getKey(block));
    }

    static boolean useObb(Block block, BlockShapeMode mode) {
        return allowsBlockModel(block)
                && (mode == BlockShapeMode.OBB || BoneHitboxConfig.forceAllBlockObb());
    }

    static boolean forceAllBlockObb() {
        return BoneHitboxConfig.forceAllBlockObb();
    }

    static boolean vanillaSlantedBlockObb() {
        return BoneHitboxConfig.vanillaSlantedBlockObb();
    }

    static Vec3 blockOffset(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getOffset(level, pos);
    }

    static boolean isAutoPartBlock(Block block) {
        return block instanceof AutoPartShapeBlock;
    }
}
