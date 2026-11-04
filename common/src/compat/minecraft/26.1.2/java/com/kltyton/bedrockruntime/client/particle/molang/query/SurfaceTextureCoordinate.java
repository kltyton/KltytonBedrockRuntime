package com.kltyton.bedrockruntime.client.particle.molang.query;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockTerrainSpriteSource;
import com.kltyton.bedrockruntime.client.particle.atlas.ParticleAtlasRevision;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

/** A captured surface sprite coordinate that follows the sprite across atlas reloads. */
final class SurfaceTextureCoordinate extends Number implements MolangStruct.DynamicNumber {
    private final Reference reference;
    private final boolean horizontal;
    private final boolean size;

    SurfaceTextureCoordinate(Reference reference, boolean horizontal, boolean size) {
        this.reference = reference;
        this.horizontal = horizontal;
        this.size = size;
    }

    @Override
    public float floatValue() {
        TextureAtlasSprite sprite = reference.sprite();
        float start = horizontal ? sprite.getU0() : sprite.getV0();
        float end = horizontal ? sprite.getU1() : sprite.getV1();
        return size ? end - start : start;
    }

    static final class Reference {
        private final Identifier spriteName;
        private final @Nullable Identifier directSource;
        private final BlockState state;
        private final Direction face;
        private final float selection;
        private @Nullable Identifier reloadKey;
        private TextureAtlasSprite sprite;
        private long revision;

        Reference(TextureAtlasSprite sprite, @Nullable Identifier directSource, @Nullable Identifier reloadKey,
                  BlockState state, Direction face, float selection) {
            this.sprite = sprite;
            this.spriteName = sprite.contents().name();
            this.directSource = directSource;
            this.reloadKey = reloadKey;
            this.state = state;
            this.face = face;
            this.selection = selection;
            this.revision = ParticleAtlasRevision.current();
        }

        TextureAtlasSprite sprite() {
            long current = ParticleAtlasRevision.current();
            if (revision != current) {
                TextureAtlas atlas = DescriptionMaterial.TERRAIN_SHEET.getAtlas();
                if (atlas.location().equals(sprite.atlasLocation())) {
                    if (reloadKey != null && directSource != null) {
                        Identifier currentSprite = BedrockTerrainSpriteSource.currentSprite(reloadKey, directSource, state, face);
                        if (currentSprite == null) reloadKey = null;
                        else sprite = atlas.getSprite(currentSprite);
                    }
                    if (directSource == null) {
                        sprite = atlas.getSprite(spriteName);
                    } else if (reloadKey == null) {
                        TextureAtlasSprite original = atlas.getSprite(directSource);
                        BedrockTerrainSpriteSource.SurfaceBinding configured =
                                BedrockTerrainSpriteSource.surfaceTextureBinding(state, face, original, selection);
                        if (configured != null) {
                            sprite = configured.sprite();
                            reloadKey = configured.reloadKey();
                        } else {
                            TextureAtlasSprite matching = BedrockTerrainSpriteSource.matchingTexture(original);
                            sprite = matching == null ? original : matching;
                        }
                    }
                }
                revision = current;
            }
            return sprite;
        }
    }

    @Override public int intValue() { return (int) floatValue(); }
    @Override public long longValue() { return (long) floatValue(); }
    @Override public double doubleValue() { return floatValue(); }
    @Override public String toString() { return Float.toString(floatValue()); }
}
