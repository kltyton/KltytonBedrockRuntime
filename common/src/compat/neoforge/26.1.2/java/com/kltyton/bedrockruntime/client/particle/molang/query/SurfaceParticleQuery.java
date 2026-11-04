package com.kltyton.bedrockruntime.client.particle.molang.query;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SurfaceParticleQuery extends MathFunction {
    public enum Kind { COLOR, COORDINATE, SIZE }
    private record SurfaceSprite(TextureAtlasSprite sprite, @Nullable Identifier directSource, @Nullable Identifier reloadKey) {}

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
                BlockTintSource tint = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
                int color = tint == null ? 0xFFFFFFFF : tint.colorAsTerrainParticle(state, level, pos);
                result.put("r", (float) ((color >> 16) & 255) / 255.0F);
                result.put("g", (float) ((color >> 8) & 255) / 255.0F);
                result.put("b", (float) (color & 255) / 255.0F);
                result.put("a", 1.0F);
            } else {
                Direction face = state.is(Blocks.SNIFFER_EGG) ? Direction.NORTH : Direction.DOWN;
                float selection = RandomSource.create(state.getSeed(pos)).nextFloat();
                SurfaceSprite sprite = surfaceSprite(state, level, pos, face, selection);
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
        return state.isFaceSturdy(level, pos, Direction.UP, SupportType.FULL);
    }

    private static SurfaceSprite surfaceSprite(BlockState state, ClientLevel level, BlockPos pos, Direction face, float selection) {
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var model = models.get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, pos, state, RandomSource.create(state.getSeed(pos)), parts);
        for (BlockStateModelPart part : parts) {
            TextureAtlasSprite sprite = spriteForFace(part.getQuads(face), face);
            if (sprite != null) return preferBedrockTexture(state, face, sprite, selection);
            sprite = spriteForFace(part.getQuads(null), face);
            if (sprite != null) return preferBedrockTexture(state, face, sprite, selection);
        }
        return preferBedrockTexture(state, face, models.getParticleMaterial(state, level, pos).sprite(), selection);
    }

    private static SurfaceSprite preferBedrockTexture(BlockState state, Direction face, TextureAtlasSprite sprite, float selection) {
        BedrockTerrainSpriteSource.SurfaceBinding configured = BedrockTerrainSpriteSource.surfaceTextureBinding(state, face, sprite, selection);
        if (configured != null) return new SurfaceSprite(configured.sprite(), sprite.contents().name(), configured.reloadKey());
        TextureAtlasSprite matching = BedrockTerrainSpriteSource.matchingTexture(sprite);
        return new SurfaceSprite(matching == null ? sprite : matching, sprite.contents().name(), null);
    }

    private static TextureAtlasSprite spriteForFace(List<BakedQuad> quads, Direction face) {
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            if (quad.direction() == face && sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
                return sprite;
            }
        }
        return null;
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
