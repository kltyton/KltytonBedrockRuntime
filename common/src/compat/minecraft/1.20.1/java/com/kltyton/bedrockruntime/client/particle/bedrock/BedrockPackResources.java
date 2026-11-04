package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.metadata.pack.PackMetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public final class BedrockPackResources extends AbstractPackResources {
    public static final String NAMESPACE = BedrockParticles.MODID + "_bedrock";
    public static final String DEFINITIONS = "bedrock_particle_definitions";
    public static final String PARTICLE_BINDINGS = "bedrock_particle_bindings";
    public static final String ATTACHABLE_PARTICLE_BINDINGS = "bedrock_attachable_particle_bindings";
    public static final ResourceLocation BLOCK_TEXTURES = new ResourceLocation(NAMESPACE, "bedrock_terrain/blocks.json");
    public static final ResourceLocation TERRAIN_TEXTURES = new ResourceLocation(NAMESPACE, "bedrock_terrain/terrain_texture.json");
    public static final ResourceLocation TERRAIN_FLIPBOOKS = new ResourceLocation(NAMESPACE, "bedrock_terrain/flipbook_textures.json");
    private static final String TEXTURES = "textures/bedrock_compat/";
    private final Path root; private final @Nullable ZipFile archive;
    public BedrockPackResources(String name, Path path) throws IOException { super(name, false); root = path; archive = Files.isDirectory(path) ? null : new ZipFile(path.toFile()); }
    public static ResourceLocation textureResource(String path) { return new ResourceLocation(NAMESPACE, TEXTURES + encode(path) + ".png"); }
    public static ResourceLocation textureSprite(String path) { return new ResourceLocation(NAMESPACE, "bedrock_compat/" + encode(path)); }
    public static @Nullable String texturePathForSprite(ResourceLocation sprite) {
        String prefix = "bedrock_compat/"; if (!sprite.getPath().startsWith(prefix)) return null;
        String path = decode(sprite.getPath().substring(prefix.length())); if (path == null || !safePath(path)) return null;
        if (sprite.getNamespace().equals(NAMESPACE)) return path.startsWith("textures/") ? path : null; return sprite.getNamespace() + ":" + path;
    }
    private static String encode(String path) { byte[] bytes = path.getBytes(StandardCharsets.UTF_8); char[] out = new char[bytes.length * 2], digits = "0123456789abcdef".toCharArray(); for (int i=0;i<bytes.length;i++){int b=bytes[i]&255;out[i*2]=digits[b>>>4];out[i*2+1]=digits[b&15];} return new String(out); }
    private static @Nullable String decode(String value) { if ((value.length()&1)!=0)return null;byte[] out=new byte[value.length()/2];for(int i=0;i<out.length;i++){int h=Character.digit(value.charAt(i*2),16),l=Character.digit(value.charAt(i*2+1),16);if(h<0||l<0)return null;out[i]=(byte)((h<<4)|l);}return new String(out,StandardCharsets.UTF_8); }
    private static boolean safePath(String path) { if(path.isEmpty()||path.startsWith("/")||path.indexOf('\\')>=0||path.indexOf('\0')>=0)return false;for(String p:path.split("/",-1))if(p.isEmpty()||p.equals(".")||p.equals(".."))return false;return true; }
    private @Nullable IoSupplier<InputStream> openPath(String path) { if(!safePath(path))return null;if(archive!=null){ZipEntry e=archive.getEntry(path);return e==null||e.isDirectory()?null:()->archive.getInputStream(e);}Path f=root.resolve(path).normalize();return f.startsWith(root.normalize())&&Files.isRegularFile(f)?IoSupplier.create(f):null; }
    @Override public @Nullable IoSupplier<InputStream> getRootResource(String... path) { if(path.length==1&&path[0].equals("pack.png"))return openPath("pack_icon.png");return openPath(String.join("/",path)); }
    @Override public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if(type!=PackType.CLIENT_RESOURCES||!id.getNamespace().equals(NAMESPACE))return null;
        if(id.equals(BLOCK_TEXTURES))return openPath("blocks.json");
        if(id.equals(TERRAIN_TEXTURES))return openPath("textures/terrain_texture.json");
        if(id.equals(TERRAIN_FLIPBOOKS))return openPath("textures/flipbook_textures.json");
        String p=id.getPath(),raw=null;
        if(p.startsWith(DEFINITIONS+"/")&&p.endsWith(".json")){raw=decode(p.substring(DEFINITIONS.length()+1,p.length()-5));if(raw==null||!raw.startsWith("particles/")||!raw.endsWith(".json"))return null;}
        else if(p.startsWith(PARTICLE_BINDINGS+"/")&&p.endsWith(".json")){raw=decode(p.substring(PARTICLE_BINDINGS.length()+1,p.length()-5));if(raw==null||!raw.startsWith("entity/")||!raw.endsWith(".json"))return null;}
        else if(p.startsWith(ATTACHABLE_PARTICLE_BINDINGS+"/")&&p.endsWith(".json")){raw=decode(p.substring(ATTACHABLE_PARTICLE_BINDINGS.length()+1,p.length()-5));if(raw==null||!raw.startsWith("attachables/")||!raw.endsWith(".json"))return null;}
        else if(p.startsWith(TEXTURES)&&p.endsWith(".png")){raw=decode(p.substring(TEXTURES.length(),p.length()-4));if(raw==null||!raw.startsWith("textures/"))return null;}return raw==null?null:openPath(raw);
    }
    @Override public void listResources(PackType type,String namespace,String directory,ResourceOutput output){
        if(type!=PackType.CLIENT_RESOURCES||!namespace.equals(NAMESPACE))return;
        String sourceDirectory;
        if(directory.equals(DEFINITIONS))sourceDirectory="particles";
        else if(directory.equals(PARTICLE_BINDINGS))sourceDirectory="entity";
        else if(directory.equals(ATTACHABLE_PARTICLE_BINDINGS))sourceDirectory="attachables";
        else return;
        if(archive!=null){archive.stream().filter(e->!e.isDirectory()).map(ZipEntry::getName).filter(p->isDefinition(p,sourceDirectory)).forEach(p->addDefinition(p,directory,output));return;}
        Path particles=root.resolve(sourceDirectory);if(!Files.isDirectory(particles))return;
        try(Stream<Path> files=Files.walk(particles)){files.filter(Files::isRegularFile).map(root::relativize).map(p->p.toString().replace('\\','/')).filter(p->isDefinition(p,sourceDirectory)).forEach(p->addDefinition(p,directory,output));}
        catch(IOException e){BedrockParticles.LOGGER.error("Cannot enumerate Bedrock particle definitions in {}",root,e);}
    }
    private static boolean isDefinition(String path,String sourceDirectory){return path.startsWith(sourceDirectory+"/")&&path.endsWith(".json")&&safePath(path);}
    private void addDefinition(String path,String directory,ResourceOutput output){IoSupplier<InputStream> resource=openPath(path);if(resource!=null)output.accept(new ResourceLocation(NAMESPACE,directory+"/"+encode(path)+".json"),resource);}
    @Override public Set<String> getNamespaces(PackType type){return type==PackType.CLIENT_RESOURCES?Set.of(NAMESPACE):Set.of();}
    @Override public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> serializer)throws IOException{
        if(serializer instanceof PackMetadataSectionSerializer){
            IoSupplier<InputStream> metadata = getRootResource("pack.mcmeta");
            if (metadata == null) return (T) new PackMetadataSection(Component.literal("Bedrock resource pack"), 15);
            JsonObject root;
            try (InputStream input = metadata.get(); InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) { root = JsonParser.parseReader(reader).getAsJsonObject(); }
            JsonObject section = root.has("pack") ? root.getAsJsonObject("pack") : new JsonObject();
            if (!section.has("description")) section.addProperty("description", "Bedrock resource pack");
            if (!section.has("pack_format")) section.addProperty("pack_format", 15);
            return (T) PackMetadataSection.TYPE.fromJson(section);
        }
        return super.getMetadataSection(serializer);
    }
    @Override public void close(){if(archive!=null)try{archive.close();}catch(IOException e){BedrockParticles.LOGGER.error("Cannot close Bedrock resource pack {}",root,e);}}
}
