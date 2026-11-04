package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
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
import java.util.function.Consumer;

public final class BedrockPackFinder implements RepositorySource {
    private final Path directory; private final DirectoryValidator validator;
    public BedrockPackFinder(Path directory, DirectoryValidator validator) { this.directory = directory; this.validator = validator; }
    @Override public void loadPacks(Consumer<Pack> output) {
        if (!Files.isDirectory(directory)) return;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) try {
                Path target = validate(entry); if (target == null) continue;
                String name = entry.getFileName().toString(); Manifest manifest = readManifest(name, target); if (manifest == null) continue;
                Pack.ResourcesSupplier supplier = packId -> { try { return new BedrockPackResources(packId, target); } catch (IOException e) { throw new IllegalStateException("Cannot open Bedrock pack " + target, e); } };
                Pack.Info info = Pack.readPackInfo("bedrock/" + name, supplier);
                if (info == null) {
                    info = new Pack.Info(Component.literal(manifest.description()), SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES), net.minecraft.world.flag.FeatureFlagSet.of());
                }
                Pack pack = Pack.create("bedrock/" + name, Component.literal(manifest.name()), false, supplier, info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, false, PackSource.DEFAULT);
                output.accept(pack);
            } catch (IOException | RuntimeException e) { BedrockParticles.LOGGER.error("Cannot discover Bedrock resource pack {}", entry, e); }
        } catch (IOException e) { BedrockParticles.LOGGER.error("Cannot enumerate resource packs in {}", directory, e); }
    }
    private Path validate(Path entry) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); List<ForbiddenSymlinkInfo> issues = new ArrayList<>(); Path target = entry;
        if (attributes.isSymbolicLink()) { validator.validateSymlink(entry, issues); if (!issues.isEmpty()) return null; Path link=Files.readSymbolicLink(entry);target=(link.isAbsolute()?link:entry.getParent().resolve(link)).normalize();attributes=Files.readAttributes(target,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS); }
        if(attributes.isDirectory()){issues.addAll(validator.validateSave(target, true));return issues.isEmpty()?target:null;}
        String filename=target.getFileName().toString().toLowerCase(java.util.Locale.ROOT);return attributes.isRegularFile()&&(filename.endsWith(".zip")||filename.endsWith(".mcpack"))?target:null;
    }
    private static Manifest readManifest(String name,Path path)throws IOException{
        try(BedrockPackResources pack=new BedrockPackResources("bedrock/"+name,path)){
            var source=pack.getRootResource("manifest.json");if(source==null)return null;
            try(InputStream input=source.get();InputStreamReader reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
                JsonObject header=BedrockJson.parse(reader).getAsJsonObject("header");if(header==null||!header.has("name"))throw new IllegalArgumentException("Missing manifest header.name");
                String title=header.get("name").getAsString();return new Manifest(title,header.has("description")?header.get("description").getAsString():title);
            }
        }
    }
    private record Manifest(String name,String description){}
}
