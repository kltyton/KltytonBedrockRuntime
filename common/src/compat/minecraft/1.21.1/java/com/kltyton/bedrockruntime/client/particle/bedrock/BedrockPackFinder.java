package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class BedrockPackFinder implements RepositorySource {
    private final Path directory;
    private final DirectoryValidator validator;

    public BedrockPackFinder(Path directory, DirectoryValidator validator) {
        this.directory = directory;
        this.validator = validator;
    }

    @Override
    public void loadPacks(Consumer<Pack> output) {
        if (!Files.isDirectory(directory)) return;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                try {
                    Path target = validate(entry);
                    if (target == null) continue;
                    String name = entry.getFileName().toString();
                    PackLocationInfo location = new PackLocationInfo("bedrock/" + name,
                            Component.literal(name), PackSource.DEFAULT, Optional.empty());
                    Manifest manifest = readManifest(location, target);
                    if (manifest == null) continue;
                    PackLocationInfo named = new PackLocationInfo(location.id(),
                            Component.literal(manifest.name()), PackSource.DEFAULT, Optional.empty());
                    Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                        @Override
                        public PackResources openPrimary(PackLocationInfo info) {
                            try {
                                return new BedrockPackResources(info, target);
                            } catch (IOException exception) {
                                throw new IllegalStateException("Cannot open Bedrock pack " + target, exception);
                            }
                        }

                        @Override
                        public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                            return openPrimary(info);
                        }
                    };
                    Pack.Metadata metadata = new Pack.Metadata(Component.literal(manifest.description()),
                            PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
                    output.accept(new Pack(named, supplier, metadata,
                            new PackSelectionConfig(false, Pack.Position.TOP, false)));
                } catch (IOException | RuntimeException exception) {
                    BedrockParticles.LOGGER.error("Cannot discover Bedrock resource pack {}", entry, exception);
                }
            }
        } catch (IOException exception) {
            BedrockParticles.LOGGER.error("Cannot enumerate resource packs in {}", directory, exception);
        }
    }

    private Path validate(Path entry) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        List<ForbiddenSymlinkInfo> issues = new ArrayList<>();
        Path target = entry;
        if (attributes.isSymbolicLink()) {
            validator.validateSymlink(entry, issues);
            if (!issues.isEmpty()) {
                BedrockParticles.LOGGER.warn("Ignoring resource pack with disallowed symbolic link {}", entry);
                return null;
            }
            Path link = Files.readSymbolicLink(entry);
            target = (link.isAbsolute() ? link : entry.getParent().resolve(link)).normalize();
            attributes = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        if (attributes.isDirectory()) {
            validator.validateKnownDirectory(target, issues);
            if (!issues.isEmpty()) {
                BedrockParticles.LOGGER.warn("Ignoring resource pack with disallowed symbolic links {}", entry);
                return null;
            }
            return target;
        }
        String filename = target.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return attributes.isRegularFile() && (filename.endsWith(".zip") || filename.endsWith(".mcpack")) ? target : null;
    }

    private static Manifest readManifest(PackLocationInfo location, Path path) throws IOException {
        try (BedrockPackResources pack = new BedrockPackResources(location, path)) {
            IoSupplier<InputStream> source = pack.getRootResource("manifest.json");
            if (source == null) return null;
            try (InputStream stream = source.get(); InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject manifest = BedrockJson.parse(reader);
                JsonObject header = manifest.getAsJsonObject("header");
                if (header == null || !header.has("name")) throw new IllegalArgumentException("Missing manifest header.name");
                String name = header.get("name").getAsString();
                String description = header.has("description") ? header.get("description").getAsString() : name;
                return new Manifest(name, description);
            }
        }
    }

    private record Manifest(String name, String description) {
    }
}
