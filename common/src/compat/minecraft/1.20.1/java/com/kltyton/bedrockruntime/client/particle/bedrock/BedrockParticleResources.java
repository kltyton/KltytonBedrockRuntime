package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.kltyton.bedrockruntime.particle.BedrockParticles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BedrockParticleResources {
    private BedrockParticleResources(){}
    public record Texture(ResourceLocation sprite,Resource resource,String sourcePath){}

    public static boolean usesBedrockItemNames(com.google.gson.JsonObject document, boolean defaultValue) {
        JsonElement value = document.get(BedrockParticles.MODID + ":item_name_format");
        if (value == null) return defaultValue;
        String format = value.getAsString();
        return switch (format) {
            case "bedrock" -> true;
            case "java" -> false;
            default -> throw new com.google.gson.JsonParseException("Invalid particle item name format: " + format);
        };
    }
    public static MolangVersion readVersion(Resource resource)throws IOException{
        var manifest=resource.source().getRootResource("manifest.json");if(manifest==null)return MolangVersion.LATEST;
        try(InputStream input=manifest.get();InputStreamReader reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            JsonObject root=BedrockJson.parse(reader),header=object(root,"header");if(header==null||!header.has("min_engine_version")){if(resource.source() instanceof BedrockPackResources)throw new IllegalArgumentException("Resource pack manifest lacks header.min_engine_version");return MolangVersion.LATEST;}
            JsonElement version=header.get("min_engine_version");
            if(version.isJsonArray()){JsonArray parts=version.getAsJsonArray();if(parts.size()!=3)throw new IllegalArgumentException("min_engine_version must contain three components");return new MolangVersion(part(parts.get(0)),part(parts.get(1)),part(parts.get(2)));}
            if(version.isJsonPrimitive()&&version.getAsJsonPrimitive().isString()){var m=java.util.regex.Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$").matcher(version.getAsString());if(m.matches())return new MolangVersion(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),m.group(4)!=null);}
            throw new IllegalArgumentException("Invalid resource pack min_engine_version");
        }
    }
    private static int part(JsonElement e){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("min_engine_version components must be integers");return e.getAsBigDecimal().intValueExact();}
    public static @Nullable String sourceTexturePath(JsonObject effect){JsonObject p=object(object(effect,"description"),"basic_render_parameters");return p==null||!p.has("texture")||!p.get("texture").isJsonPrimitive()?null:p.get("texture").getAsString();}
    public static @Nullable Texture resolveTexture(ResourceManager resources,ResourceLocation definition,JsonObject effect){
        String original=sourceTexturePath(effect);if(original==null||original.equals("atlas.terrain")||original.isBlank()||original.indexOf('\\')>=0)return null;
        boolean bedrock=definition.getNamespace().equals(BedrockPackResources.NAMESPACE)&&definition.getPath().startsWith(BedrockPackResources.DEFINITIONS+"/");
        if(!bedrock&&original.contains(":")&&!hasExt(original))return null;String namespace=definition.getNamespace(),path=original;int colon=original.indexOf(':');if(!bedrock&&colon>=0){namespace=original.substring(0,colon);path=original.substring(colon+1);}
        if(path.startsWith("/"))return null;for(String p:path.split("/",-1))if(p.isEmpty()||p.equals(".")||p.equals(".."))return null;
        List<String>bases=new ArrayList<>();if(bedrock)bases.add(path.startsWith("textures/")?path:"textures/"+path);else if(path.startsWith("textures/"))bases.add(path);else{int slash=definition.getPath().lastIndexOf('/');if(slash>=0)bases.add(definition.getPath().substring(0,slash+1)+path);bases.add("textures/particle/"+path);bases.add("textures/"+path);}
        for(String base:bases)for(String candidate:extensions(base)){ResourceLocation id=bedrock?BedrockPackResources.textureResource(candidate):ResourceLocation.tryBuild(namespace,candidate);if(id==null)continue;Optional<Resource> resource=resources.getResource(id);if(resource.isEmpty())continue;ResourceLocation sprite=bedrock?BedrockPackResources.textureSprite(candidate):new ResourceLocation(namespace,"bedrock_compat/"+hex(candidate));return new Texture(sprite,resource.get(),original);}return null;
    }

    public static Optional<Resource> findBedrockDefinition(ResourceManager resources, ResourceLocation id) throws IOException {
        List<Resource> stack = resources.getResourceStack(id);
        for (int i = stack.size() - 1; i >= 0; i--) {
            Resource resource = stack.get(i);
            try {
                if (JavaParticleResourceManager.isBedrockDefinition(resource)) return Optional.of(resource);
            } catch (com.google.gson.JsonParseException exception) {
                com.kltyton.bedrockruntime.particle.BedrockParticles.LOGGER.error(
                        "Cannot classify particle resource {} from {}", id, resource.sourcePackId(), exception);
            }
        }
        return Optional.empty();
    }

    static List<ResourceLocation> spriteAliases(Texture texture) {
        if (extensions(texture.sourcePath()).size() == 1) return List.of();
        String path = BedrockPackResources.texturePathForSprite(texture.sprite());
        if (path == null) return List.of(texture.sprite());
        if (!texture.sprite().getNamespace().equals(BedrockPackResources.NAMESPACE)) {
            path = path.substring(path.indexOf(':') + 1);
        }
        int extension = path.lastIndexOf('.');
        if (extension < 0) return List.of(texture.sprite());
        String base = path.substring(0, extension);
        List<ResourceLocation> aliases = new ArrayList<>();
        for (String candidate : extensions(base)) {
            aliases.add(texture.sprite().getNamespace().equals(BedrockPackResources.NAMESPACE)
                    ? BedrockPackResources.textureSprite(candidate)
                    : new ResourceLocation(texture.sprite().getNamespace(), "bedrock_compat/" + hex(candidate)));
        }
        return aliases;
    }

    static boolean resolvesExtensions(@Nullable String sourcePath) {
        return sourcePath != null && extensions(sourcePath).size() > 1;
    }
    public static void applyTexture(JsonObject effect,Texture texture){JsonObject p=object(object(effect,"description"),"basic_render_parameters");if(p!=null)p.addProperty("texture",texture.sprite().toString());}
    private static @Nullable JsonObject object(@Nullable JsonObject parent,String name){if(parent==null)return null;JsonElement e=parent.get(name);return e!=null&&e.isJsonObject()?e.getAsJsonObject():null;}
    private static boolean hasExt(String p){String s=p.toLowerCase(java.util.Locale.ROOT);return s.endsWith(".png")||s.endsWith(".tga")||s.endsWith(".jpg")||s.endsWith(".jpeg");}
    private static List<String> extensions(String p){return hasExt(p)?List.of(p):List.of(p+".png",p+".tga",p+".jpg",p+".jpeg");}
    private static String hex(String value){byte[] bytes=value.getBytes(StandardCharsets.UTF_8);char[] out=new char[bytes.length*2],digits="0123456789abcdef".toCharArray();for(int i=0;i<bytes.length;i++){int b=bytes[i]&255;out[i*2]=digits[b>>>4];out[i*2+1]=digits[b&15];}return new String(out);}
}
