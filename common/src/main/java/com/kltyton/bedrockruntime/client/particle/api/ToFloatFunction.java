package com.kltyton.bedrockruntime.client.particle.api;

@FunctionalInterface
public interface ToFloatFunction<T> {
    float applyAsFloat(T t);
}
