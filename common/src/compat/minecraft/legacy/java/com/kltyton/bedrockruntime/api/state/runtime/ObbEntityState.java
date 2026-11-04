package com.kltyton.bedrockruntime.api.state.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.kltyton.bedrockruntime.Constants;
import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.data.builtin.key.ObbBuiltinDataKeys;
import com.kltyton.bedrockruntime.api.data.key.ObbDataKey;
import com.kltyton.bedrockruntime.api.data.key.ObbDataRegistry;
import com.kltyton.bedrockruntime.api.registration.definition.ObbBoneDefinition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * CN: 一个 BoneHitboxEntity 的注册定义、已发现骨骼、属性覆盖和扩展数据容器。
 * EN: Container for one BoneHitboxEntity's registrations, discovered bones, attribute overlays, and extension data.
 */
public final class ObbEntityState extends ObbEntityStateBase {
    ObbEntityState(List<ObbBoneDefinition> definitions) {
        super(definitions);
    }

    public synchronized void save(CompoundTag output) {
        saveTo(new TagOutput(output));
    }

    public synchronized void load(CompoundTag input) {
        loadFrom(new TagInput(input));
    }

    private static final class TagOutput implements Output {
        private final CompoundTag tag;

        private TagOutput(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public void putInt(String key, int value) {
            tag.putInt(key, value);
        }

        @Override
        public void putString(String key, String value) {
            tag.putString(key, value);
        }

        @Override
        public ListOutput childList(String key) {
            ListTag list = new ListTag();
            tag.put(key, list);
            return new ListOutput() {
                @Override
                public Output addChild() {
                    CompoundTag child = new CompoundTag();
                    list.add(child);
                    return new TagOutput(child);
                }

                @Override
                public boolean isEmpty() {
                    return list.isEmpty();
                }
            };
        }

        @Override
        public StringListOutput stringList(String key) {
            ListTag list = new ListTag();
            tag.put(key, list);
            return value -> list.add(StringTag.valueOf(value));
        }

        @Override
        public <T> void writeData(String key, ObbDataKey<T> dataKey, Object value) {
            dataKey.codec().encodeStart(NbtOps.INSTANCE, dataValue(dataKey, value))
                    .resultOrPartial(error -> Constants.LOG.warn("Unable to encode OBB data {}: {}", dataKey.id(), error))
                    .ifPresent(encoded -> tag.put(key, encoded));
        }

        @Override
        public void writeRaw(String key, Dynamic<?> value) {
            tag.put(key, value.convert(NbtOps.INSTANCE).getValue().copy());
        }

        @Override
        public void discard(String key) {
            tag.remove(key);
        }

        @SuppressWarnings("unchecked")
        private static <T> T dataValue(ObbDataKey<T> key, Object value) {
            return (T) value;
        }
    }

    private static final class TagInput implements Input {
        private final CompoundTag tag;

        private TagInput(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public int getInt(String key, int fallback) {
            return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
        }

        @Override
        public String getString(String key, String fallback) {
            return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
        }

        @Override
        public List<Input> childList(String key) {
            List<Input> children = new ArrayList<>();
            for (Tag raw : tag.getList(key, Tag.TAG_COMPOUND)) {
                children.add(new TagInput((CompoundTag) raw));
            }
            return children;
        }

        @Override
        public List<String> stringList(String key) {
            List<String> values = new ArrayList<>();
            for (Tag raw : tag.getList(key, Tag.TAG_STRING)) {
                values.add(raw.getAsString());
            }
            return values;
        }

        @Override
        public Optional<ObbDataKey<?>> resolveDataKey(String rawId) {
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) {
                return Optional.empty();
            }
            return id.equals(Constants.id("example_part_health"))
                    ? Optional.of(ObbBuiltinDataKeys.PART_HEALTH)
                    : ObbDataRegistry.get(id);
        }

        @Override
        public <T> Optional<T> readData(String key, ObbDataKey<T> dataKey) {
            Tag encoded = tag.get(key);
            if (encoded == null) {
                return Optional.empty();
            }
            return dataKey.codec().parse(NbtOps.INSTANCE, encoded).result();
        }

        @Override
        public Optional<Dynamic<?>> readRaw(String key) {
            Tag value = tag.get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(new Dynamic<>(NbtOps.INSTANCE, value.copy()));
        }

        @Override
        public void warnUnreadData(String rawId, ObbBoneKey key) {
            Constants.LOG.warn("Preserving unread OBB bone data key {} on {}", rawId, key.displayName());
        }
    }
}
