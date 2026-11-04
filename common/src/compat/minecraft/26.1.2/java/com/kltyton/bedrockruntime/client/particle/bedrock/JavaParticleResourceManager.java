package com.kltyton.bedrockruntime.client.particle.bedrock;

import net.minecraft.resources.Identifier;
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
    private final ResourceManager delegate;
    private final Set<Identifier> spriteSets;

    public JavaParticleResourceManager(ResourceManager delegate, Set<Identifier> spriteSets) {
        this.delegate = delegate;
        this.spriteSets = spriteSets;
    }

    public static boolean isBedrockDefinition(Resource resource) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            var document = BedrockJson.parse(reader);
            return document.has("particle_effect") && document.get("particle_effect").isJsonObject();
        }
    }

    @Override
    public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
        Map<Identifier, Resource> result = delegate.listResources(directory, filter);
        if (!directory.equals("particles")) return result;
        result = new HashMap<>(result);
        for (Identifier id : List.copyOf(result.keySet())) {
            String path = id.getPath();
            if (!path.startsWith("particles/") || !path.endsWith(".json")) continue;
            Identifier particleId = id.withPath(path.substring("particles/".length(), path.length() - 5));
            if (!spriteSets.contains(particleId)) continue;
            try {
                if (!isBedrockDefinition(result.get(id))) continue;
                List<Resource> stack = delegate.getResourceStack(id);
                Resource javaResource = null;
                for (int i = stack.size() - 1; i >= 0; i--) {
                    if (!isBedrockDefinition(stack.get(i))) {
                        javaResource = stack.get(i);
                        break;
                    }
                }
                if (javaResource == null) result.remove(id);
                else result.put(id, javaResource);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to inspect particle description " + id, exception);
            }
        }
        return result;
    }

    @Override public Set<String> getNamespaces() { return delegate.getNamespaces(); }
    @Override public Optional<Resource> getResource(Identifier id) { return delegate.getResource(id); }
    @Override public List<Resource> getResourceStack(Identifier id) { return delegate.getResourceStack(id); }
    @Override public Map<Identifier, List<Resource>> listResourceStacks(String directory, Predicate<Identifier> filter) {
        return delegate.listResourceStacks(directory, filter);
    }
    @Override public Stream<PackResources> listPacks() { return delegate.listPacks(); }
}
