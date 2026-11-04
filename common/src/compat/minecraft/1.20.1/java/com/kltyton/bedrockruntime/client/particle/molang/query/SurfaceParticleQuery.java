package com.kltyton.bedrockruntime.client.particle.molang.query;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

import java.util.List;

public final class SurfaceParticleQuery extends MathFunction {
    public enum Kind { COLOR, COORDINATE, SIZE }
    private record SurfaceSprite(TextureAtlasSprite sprite, @Nullable ResourceLocation directSource, @Nullable ResourceLocation reloadKey) {}

    private final Kind kind;
    private final MolangVersion version;
    private final MathValue[] arguments;

    public SurfaceParticleQuery(Kind kind, MolangVersion version, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.version = version;
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        MolangStruct result = new MolangStruct();
        Entity entity = instance.getAttachedEntity();
        if (entity == null || entity.isRemoved() || !(instance.getLevel() instanceof ClientLevel level)) {
            return zero(result);
        }
        BlockPos.MutableBlockPos pos = BlockPos.containing((float) entity.xo, (float) entity.yo, (float) entity.zo).mutable();
        for (int i = 0; i < 10; i++, pos.move(Direction.DOWN)) {
            BlockState state = level.getBlockState(pos);
            if (!supportsSurface(state, level, pos)) continue;
            if (kind == Kind.COLOR) {
                int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
                if (color == -1) color = 0xFFFFFF;
                result.put("r", (float) ((color >> 16) & 255) / 255.0F);
                result.put("g", (float) ((color >> 8) & 255) / 255.0F);
                result.put("b", (float) (color & 255) / 255.0F);
                result.put("a", 1.0F);
            } else {
                Direction face = state.is(Blocks.SNIFFER_EGG) ? Direction.NORTH : Direction.DOWN;
                float selection = RandomSource.create(state.getSeed(pos)).nextFloat();
                SurfaceSprite sprite = surfaceSprite(state, face, selection);
                SurfaceTextureCoordinate.Reference reference = new SurfaceTextureCoordinate.Reference(
                        sprite.sprite(), sprite.directSource(), sprite.reloadKey(), state, face, selection);
                boolean size = kind == Kind.SIZE;
                result.put("u", new SurfaceTextureCoordinate(reference, true, size));
                result.put("v", new SurfaceTextureCoordinate(reference, false, size));
            }
            return result;
        }
        return zero(result);
    }

    private boolean supportsSurface(BlockState state, ClientLevel level, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.LEAVES)) return version.atLeast(1, 20, 70);
        if (state.is(Blocks.SNIFFER_EGG)) return true;
        if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock) return true;
        if (state.getBlock() instanceof StairBlock) return state.getValue(BlockStateProperties.HALF) == Half.TOP;
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static SurfaceSprite surfaceSprite(BlockState state, Direction face, float selection) {
        var renderer = Minecraft.getInstance().getBlockRenderer();
        var model = renderer.getBlockModel(state);
        TextureAtlasSprite sprite = null;
        List<BakedQuad> quads = model.getQuads(state, face, RandomSource.create(state.getSeed(BlockPos.ZERO)));
        if (!quads.isEmpty()) sprite = quads.get(0).getSprite();
        if (sprite == null) sprite = model.getParticleIcon();
        return preferBedrockTexture(state, face, sprite, selection);
    }

    private static SurfaceSprite preferBedrockTexture(BlockState state, Direction face, TextureAtlasSprite sprite, float selection) {
        BedrockTerrainSpriteSource.SurfaceBinding configured = BedrockTerrainSpriteSource.surfaceTextureBinding(state, face, sprite, selection);
        if (configured != null) return new SurfaceSprite(configured.sprite(), sprite.contents().name(), configured.reloadKey());
        TextureAtlasSprite matching = BedrockTerrainSpriteSource.matchingTexture(sprite);
        return new SurfaceSprite(matching == null ? sprite : matching, sprite.contents().name(), null);
    }

    private MolangStruct zero(MolangStruct result) {
        if (kind == Kind.COLOR) {
            result.put("r", 0.0F);
            result.put("g", 0.0F);
            result.put("b", 0.0F);
            result.put("a", 0.0F);
        } else {
            result.put("u", 0.0F);
            result.put("v", 0.0F);
        }
        return result;
    }

    @Override public double compute(MolangInstance instance) { getRaw(instance); return 0.0; }
    @Override public boolean isMutable(MathValue... values) { return true; }
    @Override public String getName() {
        return switch (kind) {
            case COLOR -> "query.surface_particle_color";
            case COORDINATE -> "query.surface_particle_texture_coordinate";
            case SIZE -> "query.surface_particle_texture_size";
        };
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
}
