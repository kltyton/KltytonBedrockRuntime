package com.kltyton.bedrockruntime.client.particle.bedrock;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public final class BedrockPackResources extends AbstractPackResources {
    public static final String NAMESPACE = BedrockParticles.MODID + "_bedrock";
    public static final String DEFINITIONS = "bedrock_particle_definitions";
    public static final String PARTICLE_BINDINGS = "bedrock_particle_bindings";
    public static final String ATTACHABLE_PARTICLE_BINDINGS = "bedrock_attachable_particle_bindings";
    public static final Identifier BLOCK_TEXTURES = Identifier.fromNamespaceAndPath(NAMESPACE, "bedrock_terrain/blocks.json");
    public static final Identifier TERRAIN_TEXTURES = Identifier.fromNamespaceAndPath(NAMESPACE, "bedrock_terrain/terrain_texture.json");
    public static final Identifier TERRAIN_FLIPBOOKS = Identifier.fromNamespaceAndPath(NAMESPACE, "bedrock_terrain/flipbook_textures.json");
    private static final String TEXTURES = "textures/bedrock_compat/";
    private final Path root;
    private final @Nullable ZipFile archive;

    public BedrockPackResources(PackLocationInfo location, Path path) throws IOException {
        super(location);
        root = path;
        archive = Files.isDirectory(path) ? null : new ZipFile(path.toFile());
    }

    public static Identifier textureResource(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, TEXTURES + encode(path) + ".png");
    }

    public static Identifier textureSprite(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "bedrock_compat/" + encode(path));
    }

    public static @Nullable String texturePathForSprite(Identifier sprite) {
        String prefix = "bedrock_compat/";
        if (!sprite.getPath().startsWith(prefix)) return null;
        String path = decode(sprite.getPath().substring(prefix.length()));
        if (path == null || !safePath(path)) return null;
        if (sprite.getNamespace().equals(NAMESPACE)) return path.startsWith("textures/") ? path : null;
        return sprite.getNamespace() + ":" + path;
    }

    private static String encode(String path) {
        return HexFormat.of().formatHex(path.getBytes(StandardCharsets.UTF_8));
    }

    private static @Nullable String decode(String encoded) {
        try {
            return new String(HexFormat.of().parseHex(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean safePath(String path) {
        if (path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0)
            return false;
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }

    private @Nullable IoSupplier<InputStream> openPath(String path) {
        if (!safePath(path)) return null;
        if (archive != null) {
            ZipEntry entry = archive.getEntry(path);
            return entry == null || entry.isDirectory() ? null : () -> archive.getInputStream(entry);
        }
        Path file = root.resolve(path).normalize();
        return file.startsWith(root.normalize()) && Files.isRegularFile(file) ? IoSupplier.create(file) : null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1) return null;
        if (path[0].equals("manifest.json")) return openPath("manifest.json");
        if (path[0].equals("pack.png")) return openPath("pack_icon.png");
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (type != PackType.CLIENT_RESOURCES || !id.getNamespace().equals(NAMESPACE)) return null;
        String path = id.getPath();
        if (id.equals(BLOCK_TEXTURES)) return openPath("blocks.json");
        if (id.equals(TERRAIN_TEXTURES)) return openPath("textures/terrain_texture.json");
        if (id.equals(TERRAIN_FLIPBOOKS)) return openPath("textures/flipbook_textures.json");
        String raw = null;
        if (path.startsWith(DEFINITIONS + "/") && path.endsWith(".json")) {
            raw = decode(path.substring(DEFINITIONS.length() + 1, path.length() - 5));
            if (raw == null || !raw.startsWith("particles/") || !raw.endsWith(".json")) return null;
        } else if (path.startsWith(PARTICLE_BINDINGS + "/") && path.endsWith(".json")) {
            raw = decode(path.substring(PARTICLE_BINDINGS.length() + 1, path.length() - 5));
            if (raw == null || !raw.startsWith("entity/") || !raw.endsWith(".json")) return null;
        } else if (path.startsWith(ATTACHABLE_PARTICLE_BINDINGS + "/") && path.endsWith(".json")) {
            raw = decode(path.substring(ATTACHABLE_PARTICLE_BINDINGS.length() + 1, path.length() - 5));
            if (raw == null || !raw.startsWith("attachables/") || !raw.endsWith(".json")) return null;
        } else if (path.startsWith(TEXTURES) && path.endsWith(".png")) {
            raw = decode(path.substring(TEXTURES.length(), path.length() - 4));
            if (raw == null || !raw.startsWith("textures/")) return null;
        }
        return raw == null ? null : openPath(raw);
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals(NAMESPACE)) return;
        String sourceDirectory;
        if (directory.equals(DEFINITIONS)) sourceDirectory = "particles";
        else if (directory.equals(PARTICLE_BINDINGS)) sourceDirectory = "entity";
        else if (directory.equals(ATTACHABLE_PARTICLE_BINDINGS)) sourceDirectory = "attachables";
        else return;
        if (archive != null) {
            archive.stream().filter(entry -> !entry.isDirectory()).map(ZipEntry::getName)
                    .filter(path -> isDefinition(path, sourceDirectory))
                    .forEach(path -> addDefinition(path, directory, output));
            return;
        }
        Path source = root.resolve(sourceDirectory);
        if (!Files.isDirectory(source)) return;
        try (Stream<Path> files = Files.walk(source)) {
            files.filter(Files::isRegularFile).map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .filter(path -> isDefinition(path, sourceDirectory))
                    .forEach(path -> addDefinition(path, directory, output));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot enumerate Bedrock particle resources in " + source, exception);
        }
    }

    private static boolean isDefinition(String path, String sourceDirectory) {
        return path.startsWith(sourceDirectory + "/") && path.endsWith(".json") && safePath(path);
    }

    private void addDefinition(String path, String directory, ResourceOutput output) {
        IoSupplier<InputStream> resource = openPath(path);
        if (resource != null) {
            output.accept(Identifier.fromNamespaceAndPath(NAMESPACE, directory + "/" + encode(path) + ".json"), resource);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(NAMESPACE) : Set.of();
    }

    @Override
    public void close() {
        if (archive != null) {
            try {
                archive.close();
            } catch (IOException exception) {
                BedrockParticles.LOGGER.error("Cannot close Bedrock resource pack {}", root, exception);
            }
        }
    }
}
