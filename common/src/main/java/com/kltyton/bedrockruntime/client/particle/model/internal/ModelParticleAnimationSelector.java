package com.kltyton.bedrockruntime.client.particle.model.internal;

import com.kltyton.bedrockruntime.client.particle.model.api.ModelParticleFrame;

import java.util.List;

/** Deterministic frame selection shared by the appearance component and the renderer. */
public final class ModelParticleAnimationSelector {
    private ModelParticleAnimationSelector() {
    }

    public static int frameCount(List<ModelParticleFrame> frames, boolean hasSingleModel) {
        if (frames != null && !frames.isEmpty()) {
            return frames.size();
        }
        return hasSingleModel ? 1 : 0;
    }

    public static int frame(double age, double lifetime, int frameCount, float framesPerSecond,
                            boolean stretchToLifetime, boolean loop) {
        if (frameCount <= 1) {
            return 0;
        }
        double frame;
        if (stretchToLifetime) {
            double duration = lifetime <= 0.0 ? 1.0 : lifetime;
            frame = Math.floor(frameCount * Math.max(0.0, age) / duration);
            if (frame >= frameCount) {
                frame = frameCount - 1;
            }
        } else {
            double fps = framesPerSecond <= 0.0F ? 1.0 : framesPerSecond;
            frame = Math.floor(Math.max(0.0, age) * fps);
            if (loop) {
                frame %= frameCount;
            } else if (frame >= frameCount) {
                frame = frameCount - 1;
            }
        }
        return (int) Math.max(0, Math.min(frameCount - 1, frame));
    }

    public static String modelAt(List<ModelParticleFrame> frames, String singleModel, int frame, double random) {
        if (frames == null || frames.isEmpty()) {
            return singleModel == null ? "" : singleModel;
        }
        int index = Math.max(0, Math.min(frame, frames.size() - 1));
        double mixed = (random + index * 0.6180339887498949D) % 1.0D;
        return frames.get(index).select(mixed);
    }

}


