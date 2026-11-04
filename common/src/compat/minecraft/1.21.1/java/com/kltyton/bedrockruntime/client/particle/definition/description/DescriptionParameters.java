package com.kltyton.bedrockruntime.client.particle.definition.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import com.kltyton.bedrockruntime.client.particle.PSDiagnostics;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.access.ITextureAtlasSprite;

public class DescriptionParameters {
    public static final ResourceLocation MISSING_TEXTURE = BedrockParticles.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.optionalFieldOf("material", DescriptionMaterial.CUSTOM).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.optionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));
    private final DescriptionMaterial material;
    private final ResourceLocation texture;
    private int textureIndex = -1;

    public DescriptionParameters(DescriptionMaterial material, ResourceLocation texture) {
        this.material = material;
        this.texture = texture;
    }

    public DescriptionMaterial material() {
        return material;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public ResourceLocation bindTexture(int index) {
        textureIndex = index;
        return texture;
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    public TextureAtlasSprite getTexture() {
        TextureAtlas atlas = material.getAtlas();
        ResourceLocation resolvedTexture = resolveTextureId(atlas, texture);
        TextureAtlasSprite sprite = atlas.getSprite(resolvedTexture);
        boolean missing = MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
        PSDiagnostics.infoOnce("texture:" + material + ":" + texture, "texture lookup texture={} resolvedTexture={} material={} spriteName={} missing={} spriteSize={}x{} atlasSize={}x{} u0={} u1={} v0={} v1={}",
                texture,
                resolvedTexture,
                material,
                sprite.contents().name(),
                missing,
                sprite.contents().width(),
                sprite.contents().height(),
                ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginX(),
                ((ITextureAtlasSprite) sprite).kltyton_bedrock_runtime$getOriginY(),
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1()
        );
        return sprite;
    }

    public TextureAtlasSprite getTexture(boolean wholeTerrainAtlas) {
        return wholeTerrainAtlas
                ? DescriptionMaterial.TERRAIN_SHEET.getAtlas().getSprite(MissingTextureAtlasSprite.getLocation())
                : getTexture();
    }

    private static ResourceLocation resolveTextureId(TextureAtlas atlas, ResourceLocation id) {
        TextureAtlasSprite sprite = atlas.getSprite(id);
        if (!MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return id;
        }
        if (atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            String path = id.getPath();
            if (path.startsWith("textures/block/")) path = path.substring("textures/".length());
            else if (!path.startsWith("block/")) path = "block/" + path;
            ResourceLocation blockTexture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
            if (!MissingTextureAtlasSprite.getLocation().equals(atlas.getSprite(blockTexture).contents().name())) {
                return blockTexture;
            }
        }
        ResourceLocation fallback = bedrockTextureFallback(id);
        if (fallback.equals(id)) {
            return id;
        }
        TextureAtlasSprite fallbackSprite = atlas.getSprite(fallback);
        if (!MissingTextureAtlasSprite.getLocation().equals(fallbackSprite.contents().name())) {
            PSDiagnostics.infoOnce("texture-fallback:" + id, "texture fallback original={} fallback={}", id, fallback);
            return fallback;
        }
        PSDiagnostics.warnOnce("texture-missing:" + id, "texture missing original={} fallback={}", id, fallback);
        return id;
    }

    private static ResourceLocation bedrockTextureFallback(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("textures/particle/")) {
            path = path.substring("textures/particle/".length());
        } else if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        } else {
            return id;
        }
        String namespace = id.getNamespace().equals("minecraft") ? BedrockParticles.MODID : id.getNamespace();
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
