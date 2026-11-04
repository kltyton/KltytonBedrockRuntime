package com.kltyton.bedrockruntime.client.particle.api;


import java.util.concurrent.Executor;

public abstract class MolangParticleLoadEvent {
    private final Executor executor;

    public MolangParticleLoadEvent(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public static class Pre extends MolangParticleLoadEvent {

        @FunctionalInterface
        public interface Callback {
            void onLoad(Pre event);
        }

        public Pre(Executor executor) {
            super(executor);
        }
    }

    /// In Game Executor {@link net.minecraft.client.Minecraft}
    public static class Post extends MolangParticleLoadEvent {

        @FunctionalInterface
        public interface Callback {
            void onLoad(Post event);
        }

        public Post(Executor executor) {
            super(executor);
        }
    }
}
