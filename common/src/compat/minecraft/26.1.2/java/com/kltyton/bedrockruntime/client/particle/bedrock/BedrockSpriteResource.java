package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/** Supplies transformed images to the active sprite loader while preserving other resource metadata. */
public final class BedrockSpriteResource {
    private BedrockSpriteResource() {
    }

    public static Resource fromImage(Resource source, ResourceMetadata metadata, NativeImage image,
            Optional<AnimationMetadataSection> animation) throws IOException {
        BufferedImage pixels = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        pixels.setRGB(0, 0, image.getWidth(), image.getHeight(), image.getPixels(), 0, image.getWidth());
        byte[] png;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            if (!ImageIO.write(pixels, "png", output)) throw new IOException("No PNG writer for transformed terrain sprite");
            output.flush();
            png = bytes.toByteArray();
        }
        ResourceMetadata animationMetadata = animation
                .map(value -> ResourceMetadata.of(AnimationMetadataSection.TYPE, value))
                .orElse(ResourceMetadata.EMPTY);
        ResourceMetadata combined = new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionType<T> type) {
                return type.equals(AnimationMetadataSection.TYPE)
                        ? animationMetadata.getSection(type) : metadata.getSection(type);
            }
        };
        return new Resource(source.source(), () -> new ByteArrayInputStream(png), () -> combined);
    }
}
