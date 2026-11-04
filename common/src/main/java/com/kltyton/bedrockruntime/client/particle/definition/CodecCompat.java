package com.kltyton.bedrockruntime.client.particle.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * DFU compatibility helpers for codecs used by particle definitions.
 *
 * <p>Older DataFixerUpper releases (the 1.20.1 runtime) do not expose
 * {@code Codec.list(element, min, max)} or {@code Codec.lenientOptionalFieldOf}.
 * These helpers preserve the newer semantics while staying source-compatible with
 * every DFU version shipped by the supported Minecraft targets.</p>
 */
public final class CodecCompat {
    private CodecCompat() {
    }

    public static <E> Codec<List<E>> list(Codec<E> element, int min, int max) {
        return new com.kltyton.bedrockruntime.client.particle.definition.codec.BoundedListCodec<>(element, min, max);
    }

    public static <A> MapCodec<Optional<A>> lenientOptionalFieldOf(Codec<A> codec, String name) {
        return new LenientOptionalFieldCodec<>(codec, name);
    }

    public static <A> MapCodec<A> lenientOptionalFieldOf(Codec<A> codec, String name, A defaultValue) {
        return lenientOptionalFieldOf(codec, name).xmap(
                value -> value.orElse(defaultValue),
                value -> Objects.equals(value, defaultValue) ? Optional.empty() : Optional.of(value));
    }

    private static final class LenientOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final Codec<A> elementCodec;
        private final String name;

        private LenientOptionalFieldCodec(Codec<A> elementCodec, String name) {
            this.elementCodec = elementCodec;
            this.name = name;
        }

        @Override
        public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get(name);
            if (value == null) {
                return DataResult.success(Optional.empty());
            }
            DataResult<A> parsed = elementCodec.parse(ops, value);
            if (parsed.error().isPresent()) {
                return DataResult.success(Optional.empty());
            }
            return parsed.map(Optional::of);
        }

        @Override
        public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input.isPresent()) {
                return prefix.add(name, elementCodec.encodeStart(ops, input.get()));
            }
            return prefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(name));
        }

        @Override
        public boolean equals(Object other) {
            return other == this || (other instanceof LenientOptionalFieldCodec<?> that
                    && name.equals(that.name)
                    && elementCodec.equals(that.elementCodec));
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, elementCodec);
        }

        @Override
        public String toString() {
            return "LenientOptionalFieldCodec[" + name + ": " + elementCodec + ']';
        }
    }
}
