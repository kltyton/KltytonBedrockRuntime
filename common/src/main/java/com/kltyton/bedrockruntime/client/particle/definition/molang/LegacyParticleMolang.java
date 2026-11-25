package com.kltyton.bedrockruntime.client.particle.definition.molang;

import com.kltyton.bedrockruntime.BuildInfo;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/** The original object representation of a particle expression. Its revision is distinct from the pack engine version. */
record LegacyParticleMolang(String expression, Optional<Integer> version) {
    private static final Codec<LegacyParticleMolang> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("expression").forGetter(LegacyParticleMolang::expression),
            Codec.INT.optionalFieldOf("version").forGetter(LegacyParticleMolang::version)
    ).apply(instance, LegacyParticleMolang::new));

    static <T> Codec<T> extend(Codec<T> scalar, Function<LegacyParticleMolang, T> decode,
                               Function<T, @Nullable LegacyParticleMolang> encode) {
        if (!BuildInfo.MINECRAFT_VERSION.equals("26.2")) return scalar;
        return Codec.either(scalar, CODEC).xmap(value -> value.map(Function.identity(), decode), value -> {
            var legacy = encode.apply(value);
            return legacy == null ? Either.left(value) : Either.right(legacy);
        });
    }
}
