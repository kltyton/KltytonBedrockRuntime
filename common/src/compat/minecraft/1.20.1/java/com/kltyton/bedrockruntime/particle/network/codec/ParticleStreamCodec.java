package com.kltyton.bedrockruntime.particle.network.codec;

import io.netty.buffer.ByteBuf;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** Encodes particle values through the native buffer API on versions without StreamCodec. */
public record ParticleStreamCodec<B extends ByteBuf, T>(BiConsumer<B, T> encoder, Function<B, T> decoder) {
    public void encode(B buffer, T value) {
        encoder.accept(buffer, value);
    }

    public T decode(B buffer) {
        return decoder.apply(buffer);
    }
}
