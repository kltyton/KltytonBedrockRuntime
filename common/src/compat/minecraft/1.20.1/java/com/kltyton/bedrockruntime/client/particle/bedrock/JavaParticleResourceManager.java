package com.kltyton.bedrockruntime.client.particle.bedrock;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Keeps Java particle descriptions visible to the vanilla particle loader. */
public final class JavaParticleResourceManager implements ResourceManager {
    private final ResourceManager delegate; private final Set<ResourceLocation> spriteSets;
    public JavaParticleResourceManager(ResourceManager delegate, Set<ResourceLocation> spriteSets) { this.delegate = delegate; this.spriteSets = spriteSets; }
    public static boolean isBedrockDefinition(Resource resource) throws IOException { try (Reader reader = resource.openAsReader()) { var doc = BedrockJson.parse(reader); return doc.has("particle_effect") && doc.get("particle_effect").isJsonObject(); } }
    @Override public Map<ResourceLocation, Resource> listResources(String directory, Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, Resource> result = delegate.listResources(directory, filter); if (!directory.equals("particles")) return result; result = new HashMap<>(result);
        for (ResourceLocation id : List.copyOf(result.keySet())) {
            String path=id.getPath(); if(!path.startsWith("particles/")||!path.endsWith(".json"))continue;
            ResourceLocation particle=new ResourceLocation(id.getNamespace(),path.substring(9,path.length()-5));if(!spriteSets.contains(particle))continue;
            try { if(!isBedrockDefinition(result.get(id)))continue;List<Resource> stack=delegate.getResourceStack(id);Resource javaResource=null;for(int i=stack.size()-1;i>=0;i--)if(!isBedrockDefinition(stack.get(i))){javaResource=stack.get(i);break;}if(javaResource==null)result.remove(id);else result.put(id,javaResource); }
            catch(IOException e){throw new IllegalStateException("Failed to inspect particle description "+id,e);}
        }
        return result;
    }
    @Override public Set<String> getNamespaces(){return delegate.getNamespaces();}
    @Override public Optional<Resource> getResource(ResourceLocation id){return delegate.getResource(id);}
    @Override public List<Resource> getResourceStack(ResourceLocation id){return delegate.getResourceStack(id);}
    @Override public Map<ResourceLocation,List<Resource>> listResourceStacks(String directory,Predicate<ResourceLocation> filter){return delegate.listResourceStacks(directory,filter);}
    @Override public Stream<PackResources> listPacks(){return delegate.listPacks();}
}
