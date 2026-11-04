package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public class ExtendMutableSpriteSet implements SpriteSet {
    private final List<TextureAtlasSprite> sprites = new ArrayList<>();
    private TextureAtlasSprite missing;
    private final Map<ResourceLocation, TextureAtlasSprite> byName = new LinkedHashMap<>();

    public TextureAtlasSprite get(int index) {
        if (index < 0 || index >= sprites.size()) {
            return missing;
        }
        return sprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        if (byName.putIfAbsent(sprite.contents().name(), sprite) == null) sprites.add(sprite);
    }

    public void clear() {
        sprites.clear();
        byName.clear();
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }

    public void rebind(List<TextureAtlasSprite> replacement) {
        clear();
        for (TextureAtlasSprite sprite : replacement) addSprite(sprite);
    }

    @Override
    public TextureAtlasSprite get(int index, int max) {
        if (sprites.isEmpty()) {
            return missing;
        }
        int resolved = max <= 0 ? 0 : index * (sprites.size() - 1) / max;
        return get(resolved);
    }

    @Override
    public TextureAtlasSprite get(RandomSource random) {
        if (sprites.isEmpty()) {
            return missing;
        }
        return sprites.get(random.nextInt(sprites.size()));
    }

    public TextureAtlasSprite first() {
        return sprites.isEmpty() ? missing : sprites.get(0);
    }
}
