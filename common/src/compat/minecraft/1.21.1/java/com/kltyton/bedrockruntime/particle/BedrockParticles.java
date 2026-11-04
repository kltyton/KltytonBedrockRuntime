package com.kltyton.bedrockruntime.particle;

import com.kltyton.bedrockruntime.client.particle.runtime.MolangParticleOption;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Common Bedrock particle entrypoint. This class must never reference client-only bootstrap code. */
public final class BedrockParticles {
    public static final String MODID = "kltyton_bedrock_runtime";
    public static final Logger LOGGER = LoggerFactory.getLogger("KltBedrockRuntime/Particles");
    public static final boolean GECKOLIB_LOADED = true;

    public static final ParticleType<MolangParticleOption> MOLANG = new ParticleType<>(false) {
        @Override
        public MapCodec<MolangParticleOption> codec() {
            return MolangParticleOption.CODEC;
        }

        @Override
        public StreamCodec<ByteBuf, MolangParticleOption> streamCodec() {
            return MolangParticleOption.STREAM_CODEC;
        }
    };

    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list)
    );

    private BedrockParticles() {
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static <A> MapCodec<A> lenientOptionalFieldOf(Codec<A> codec, String name, Supplier<? extends A> value) {
        return Codec.optionalField(name, codec, true).xmap(
                optional -> optional.orElseGet(value),
                result -> Objects.equals(result, value.get()) ? Optional.empty() : Optional.of(result));
    }
}
