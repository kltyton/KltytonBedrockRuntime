package com.kltyton.bedrockruntime.client.particle.runtime;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtendMutableSpriteSet implements SpriteSet {
    private final List<TextureAtlasSprite> sprites = new ArrayList<>();
    private final Map<Identifier, TextureAtlasSprite> byName = new LinkedHashMap<>();
    private final Map<SpriteKey, TextureAtlasSprite> byAtlasAndName = new LinkedHashMap<>();
    private TextureAtlasSprite missing;

    private record SpriteKey(Identifier atlas, Identifier texture) {}

    public TextureAtlasSprite get(int index) {
        if (index < 0 || index >= sprites.size()) {
            return missing;
        }
        return sprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        byAtlasAndName.putIfAbsent(new SpriteKey(sprite.atlasLocation(), sprite.contents().name()), sprite);
        if (byName.putIfAbsent(sprite.contents().name(), sprite) == null) {
            sprites.add(sprite);
        }
    }

    public void clear() {
        sprites.clear();
        byName.clear();
        byAtlasAndName.clear();
    }

    public TextureAtlasSprite get(Identifier texture) {
        return byName.getOrDefault(texture, missing);
    }

    public @Nullable TextureAtlasSprite find(Identifier texture) {
        return byName.get(texture);
    }

    public @Nullable TextureAtlasSprite find(Identifier atlas, Identifier texture) {
        return byAtlasAndName.get(new SpriteKey(atlas, texture));
    }

    public int size() {
        return byName.size();
    }

    public void rebind(List<TextureAtlasSprite> replacement) {
        clear();
        sprites.addAll(replacement);
        for (TextureAtlasSprite sprite : replacement) {
            byName.putIfAbsent(sprite.contents().name(), sprite);
            byAtlasAndName.putIfAbsent(new SpriteKey(sprite.atlasLocation(), sprite.contents().name()), sprite);
        }
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
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

    @Override
    public TextureAtlasSprite first() {
        return sprites.isEmpty() ? missing : sprites.getFirst();
    }
}
