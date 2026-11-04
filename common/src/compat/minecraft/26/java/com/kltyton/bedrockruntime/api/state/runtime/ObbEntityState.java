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

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * CN: 一个 BoneHitboxEntity 的注册定义、已发现骨骼、属性覆盖和扩展数据容器。
 * EN: Container for one BoneHitboxEntity's registrations, discovered bones, attribute overlays, and extension data.
 */
public final class ObbEntityState extends ObbEntityStateBase {
    ObbEntityState(List<ObbBoneDefinition> definitions) {
        super(definitions);
    }

    public synchronized void save(ValueOutput output) {
        saveTo(new ValueOutputAdapter(output));
    }

    public synchronized void load(ValueInput input) {
        loadFrom(new ValueInputAdapter(input));
    }

    private static final class ValueOutputAdapter implements Output {
        private final ValueOutput output;

        private ValueOutputAdapter(ValueOutput output) {
            this.output = output;
        }

        @Override
        public void putInt(String key, int value) {
            output.putInt(key, value);
        }

        @Override
        public void putString(String key, String value) {
            output.putString(key, value);
        }

        @Override
        public ListOutput childList(String key) {
            ValueOutput.ValueOutputList list = output.childrenList(key);
            return new ListOutput() {
                @Override
                public Output addChild() {
                    return new ValueOutputAdapter(list.addChild());
                }

                @Override
                public boolean isEmpty() {
                    return list.isEmpty();
                }
            };
        }

        @Override
        public StringListOutput stringList(String key) {
            ValueOutput.TypedOutputList<String> list = output.list(key, Codec.STRING);
            return list::add;
        }

        @Override
        public <T> void writeData(String key, ObbDataKey<T> dataKey, Object value) {
            output.store(key, dataKey.codec(), dataValue(dataKey, value));
        }

        @Override
        public void writeRaw(String key, Dynamic<?> value) {
            output.store(key, Codec.PASSTHROUGH, value);
        }

        @Override
        public void discard(String key) {
            output.discard(key);
        }

        @SuppressWarnings("unchecked")
        private static <T> T dataValue(ObbDataKey<T> key, Object value) {
            return (T) value;
        }
    }

    private static final class ValueInputAdapter implements Input {
        private final ValueInput input;

        private ValueInputAdapter(ValueInput input) {
            this.input = input;
        }

        @Override
        public int getInt(String key, int fallback) {
            return input.getIntOr(key, fallback);
        }

        @Override
        public String getString(String key, String fallback) {
            return input.getStringOr(key, fallback);
        }

        @Override
        public List<Input> childList(String key) {
            List<Input> children = new ArrayList<>();
            for (ValueInput child : input.childrenListOrEmpty(key)) {
                children.add(new ValueInputAdapter(child));
            }
            return children;
        }

        @Override
        public List<String> stringList(String key) {
            List<String> values = new ArrayList<>();
            input.listOrEmpty(key, Codec.STRING).forEach(values::add);
            return values;
        }

        @Override
        public Optional<ObbDataKey<?>> resolveDataKey(String rawId) {
            Identifier id = Identifier.tryParse(rawId);
            if (id == null) {
                return Optional.empty();
            }
            return id.equals(Constants.id("example_part_health"))
                    ? Optional.of(ObbBuiltinDataKeys.PART_HEALTH)
                    : ObbDataRegistry.get(id);
        }

        @Override
        public <T> Optional<T> readData(String key, ObbDataKey<T> dataKey) {
            return input.read(key, dataKey.codec());
        }

        @Override
        public Optional<Dynamic<?>> readRaw(String key) {
            return input.read(key, Codec.PASSTHROUGH);
        }

        @Override
        public void warnUnreadData(String rawId, ObbBoneKey key) {
            Constants.LOG.warn("Preserving unread OBB bone data key {} on {}", rawId, key.displayName());
        }
    }
}
