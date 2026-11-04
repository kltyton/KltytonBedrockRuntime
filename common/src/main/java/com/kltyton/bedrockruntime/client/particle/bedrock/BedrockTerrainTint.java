package com.kltyton.bedrockruntime.client.particle.bedrock;

/** Applies a Bedrock terrain tint map while preparing atlas sprites. */
final class BedrockTerrainTint {
    record Channels(int red, int green, int blue, int alpha) {
        int get(int channel) {
            return switch (channel) {
                case 0 -> red;
                case 1 -> green;
                case 2 -> blue;
                case 3 -> alpha;
                default -> throw new IndexOutOfBoundsException(channel);
            };
        }
    }

    private final Channels base;
    private final Channels target;
    private final float[] source = new float[4];
    private final float[] background = new float[4];
    private final float[] mask = new float[4];
    private final float[] average = new float[3];
    private final float[] color = new float[4];
    private final float[] shifted = new float[3];
    private final float[] sumColor = new float[3];
    private final float[] difference = new float[3];
    private final float[] differenceHsl = new float[3];
    private final float[] baseHsl = new float[3];
    private final float[] differenceRgb = new float[3];
    private final float[] sourceLab = new float[3];
    private final float[] baseLab = new float[3];
    private final float[] targetLab = new float[3];
    private final float[] labRgb = new float[3];

    BedrockTerrainTint(Channels base, Channels target) {
        this.base = base;
        this.target = target;
    }

    int apply(int pixel, int tintMap, int previousPixel, Integer overlay) {
        unpackPixel(pixel, source);
        unpackPixel(previousPixel, background);
        unpackPixel(tintMap, mask);
        float total = mask[0] + mask[1] + mask[2] + mask[3];
        float divisor = total <= 0.0F ? 1.0F : total;
        for (int component = 0; component < 3; component++) {
            float value = 0.0F;
            for (int channel = 0; channel < 4; channel++) {
                value += mask[channel] * component(base.get(channel), component);
            }
            average[component] = value / divisor;
        }
        float remaining = Math.max(1.0F - total, 0.0F);
        for (int component = 0; component < 3; component++) {
            sumColor[component] = remaining * source[component];
        }
        for (int channel = 0; channel < 4; channel++) {
            int selected = target.get(channel);
            if (mask[channel] > 0.0F && selected != 0) {
                unpackColor(selected, color);
                shift(source, average, color, shifted);
            } else {
                System.arraycopy(source, 0, shifted, 0, 3);
            }
            for (int component = 0; component < 3; component++) {
                sumColor[component] += mask[channel] * shifted[component];
            }
        }
        divisor = remaining + total;
        float alpha = source[3];
        for (int component = 0; component < 3; component++) {
            source[component] = clamp(alpha * (sumColor[component] / divisor)
                    + (1.0F - alpha) * background[component]);
        }
        alpha = clamp(alpha + (1.0F - alpha) * background[3]);
        if (overlay != null) {
            for (int component = 0; component < 3; component++) {
                float value = source[component];
                source[component] = (1.0F - alpha) * value
                        + component(overlay, component) * value * alpha;
            }
            alpha = 1.0F;
        }
        return byteValue(alpha) << 24 | byteValue(source[0]) << 16
                | byteValue(source[1]) << 8 | byteValue(source[2]);
    }

    private void shift(float[] input, float[] baseColor, float[] targetColor, float[] output) {
        for (int i = 0; i < 3; i++) difference[i] = baseColor[i] - input[i];
        toHsl(difference, differenceHsl);
        toHsl(baseColor, baseHsl);
        differenceHsl[1] *= 0.5F * (smooth(baseHsl[0]) + smooth(baseHsl[1]));
        fromHsl(differenceHsl, differenceRgb);

        toLab(input, sourceLab);
        toLab(baseColor, baseLab);
        toLab(targetColor, targetLab);
        float light = sourceLab[0] - baseLab[0] + targetLab[0];
        float chroma = sourceLab[1] - baseLab[1] + targetLab[1];
        float hue = sourceLab[2] - baseLab[2] + targetLab[2];
        fromLab(light, chroma, hue, labRgb);

        float luminance = luminance(input) - luminance(baseColor) + 0.5F;
        float amount = luminance >= 0.5F ? 2.0F * (1.0F - luminance) : 2.0F * luminance;
        float chromaMix = smooth(targetLab[1] / 133.8158721923828F);
        float targetMix = smooth((targetColor[0] + targetColor[1] + targetColor[2]) / 3.0F);
        for (int i = 0; i < 3; i++) {
            float overlay = luminance >= 0.5F ? 1.0F - amount * (1.0F - targetColor[i])
                    : amount * targetColor[i];
            output[i] = chromaMix * overlay + (1.0F - chromaMix)
                    * ((targetColor[i] - differenceRgb[i]) * (1.0F - targetMix) + targetMix * labRgb[i]);
        }
    }

    private static void toHsl(float[] rgb, float[] hsl) {
        float max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        float min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        float delta = max - min;
        float light = (max + min) * 0.5F;
        float hue = 0.0F;
        float saturation = 0.0F;
        if (delta != 0.0F) {
            saturation = delta / (light < 0.5F ? max + min : 2.0F - max - min);
            float r = ((max - rgb[0]) / 6.0F + delta * 0.5F) / delta;
            float g = ((max - rgb[1]) / 6.0F + delta * 0.5F) / delta;
            float b = ((max - rgb[2]) / 6.0F + delta * 0.5F) / delta;
            hue = rgb[0] == max ? b - g : rgb[1] == max ? 1.0F / 3.0F + r - b
                    : 2.0F / 3.0F + g - r;
            if (hue < 0.0F) hue += 1.0F;
            if (hue > 1.0F) hue -= 1.0F;
        }
        hsl[0] = hue;
        hsl[1] = saturation;
        hsl[2] = light;
    }

    private static void fromHsl(float[] hsl, float[] rgb) {
        float hue = hsl[0];
        float saturation = hsl[1];
        float light = hsl[2];
        if (saturation == 0.0F) {
            rgb[0] = light;
            rgb[1] = light;
            rgb[2] = light;
            return;
        }
        float upper = light < 0.5F ? (1.0F + saturation) * light
                : light + saturation - saturation * light;
        float lower = 2.0F * light - upper;
        rgb[0] = hueChannel(lower, upper, hue + 1.0F / 3.0F);
        rgb[1] = hueChannel(lower, upper, hue);
        rgb[2] = hueChannel(lower, upper, hue - 1.0F / 3.0F);
    }

    private static float hueChannel(float lower, float upper, float hue) {
        if (hue < 0.0F) hue += 1.0F;
        if (hue > 1.0F) hue -= 1.0F;
        if (hue * 6.0F < 1.0F) return lower + (upper - lower) * 6.0F * hue;
        if (hue * 2.0F < 1.0F) return upper;
        if (hue * 3.0F < 2.0F) return lower + (upper - lower) * (2.0F / 3.0F - hue) * 6.0F;
        return lower;
    }

    private static void toLab(float[] rgb, float[] lab) {
        float r = linear(rgb[0]);
        float g = linear(rgb[1]);
        float b = linear(rgb[2]);
        float x = labPivot((0.4124F * r + 0.3576F * g + 0.1805F * b) * 100.0F / 95.047F);
        float y = labPivot(0.2126F * r + 0.7152F * g + 0.0722F * b);
        float z = labPivot((0.0193F * r + 0.1192F * g + 0.9505F * b) * 100.0F / 108.883F);
        float a = (x - y) * 500.0F;
        float c = (y - z) * 200.0F;
        lab[0] = 116.0F * y - 16.0F;
        lab[1] = (float) Math.sqrt(c * c + a * a);
        lab[2] = (float) Math.atan2(c, a);
    }

    private static void fromLab(float light, float chroma, float hue, float[] rgb) {
        float y = (light + 16.0F) / 116.0F;
        float x = y + (float) Math.cos(hue) * chroma / 500.0F;
        float z = y - (float) Math.sin(hue) * chroma / 200.0F;
        x = inverseLabPivot(x) * 95.047F / 100.0F;
        y = inverseLabPivot(y);
        z = inverseLabPivot(z) * 108.883F / 100.0F;
        rgb[0] = gamma(3.2406F * x - 1.5372F * y - 0.4986F * z);
        rgb[1] = gamma(-0.9689F * x + 1.8758F * y + 0.0415F * z);
        rgb[2] = gamma(0.0557F * x - 0.204F * y + 1.057F * z);
    }

    private static float linear(float value) {
        return value <= 0.04045 ? value / 12.92F : (float) Math.pow((value + 0.055F) / 1.055F, 2.4F);
    }

    private static float gamma(float value) {
        return value <= 0.0031308 ? value * 12.92F : 1.055F * (float) Math.pow(value, 1.0F / 2.4F) - 0.055F;
    }

    private static float labPivot(float value) {
        return value <= 0.008856F ? value * 7.787F + 0.13793103396892548F
                : (float) Math.pow(value, 1.0F / 3.0F);
    }

    private static float inverseLabPivot(float value) {
        float cube = value * value * value;
        return cube > 0.008856F ? cube : (value - 0.13793103396892548F) / 7.787F;
    }

    private static float smooth(float value) {
        return (3.0F - 2.0F * value) * value * value;
    }

    private static float luminance(float[] rgb) {
        return rgb[0] * 0.299F + rgb[1] * 0.587F + rgb[2] * 0.114F;
    }

    private static float component(int color, int index) {
        return ((color >> (16 - 8 * index)) & 255) / 255.0F;
    }

    private static void unpackColor(int pixel, float[] result) {
        result[0] = component(pixel, 0);
        result[1] = component(pixel, 1);
        result[2] = component(pixel, 2);
        result[3] = (pixel >>> 24) / 255.0F;
    }

    private static void unpackPixel(int pixel, float[] result) {
        float scale = 1.0F / 255.0F;
        result[0] = (pixel >> 16 & 255) * scale;
        result[1] = (pixel >> 8 & 255) * scale;
        result[2] = (pixel & 255) * scale;
        result[3] = (pixel >>> 24) * scale;
    }

    private static int byteValue(float value) {
        return (int) (clamp(value) * 255.0F);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
