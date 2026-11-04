package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.kltyton.bedrockruntime.client.particle.definition.Tuple;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;

import java.util.*;
import java.util.stream.Stream;

public record ParticleAppearanceTinting(Color color, ColorField colorField) implements IParticleComponent {
    public static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.withDefaultNamespace("particle_appearance_tinting");
    public static final Codec<ParticleAppearanceTinting> CODEC = Codec.either(Color.CODEC, ColorField.CODEC).xmap(
            either -> either.map(c -> new ParticleAppearanceTinting(c, ColorField.EMPTY), f -> new ParticleAppearanceTinting(Color.EMPTY, f)),
            tinting -> tinting.colorField == ColorField.EMPTY ? Either.left(tinting.color) : Either.right(tinting.colorField)
    ).fieldOf("color").codec();

    @Override
    public Codec<ParticleAppearanceTinting> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        List<FloatMolangExp> collect = new ArrayList<>(color.gradient.map.values().stream().flatMap(field -> Stream.of(field.red, field.green, field.blue, field.alpha)).toList());
        collect.add(color.interpolant);
        collect.add(colorField.red);
        collect.add(colorField.green);
        collect.add(colorField.blue);
        collect.add(colorField.alpha);
        return List.copyOf(collect);
    }

    private float[] getCalculatedColor(IMolangParticleInstance instance, ArrayList<Tuple<Float, ColorField>> list, float ratio) {
        Tuple<Float, ColorField> first = list.get(0);
        if (list.size() == 1 || ratio <= first.getA() + Math.ulp(1.0F)) {
            return first.getB().calculate(instance);
        }
        Tuple<Float, ColorField> last = list.get(list.size() - 1);
        if (ratio >= last.getA() - Math.ulp(1.0F)) return last.getB().calculate(instance);
        int nextIndex = 1;
        while (nextIndex < list.size() - 1 && !(ratio < list.get(nextIndex).getA())) nextIndex++;
        Tuple<Float, ColorField> previous = list.get(nextIndex - 1);
        Tuple<Float, ColorField> next = list.get(nextIndex);
        ColorField start = previous.getB();
        ColorField end = next.getB();
        float nextRed = end.red.calculate(instance);
        float red = start.red.calculate(instance);
        float interpolation = (ratio - previous.getA()) / (next.getA() - previous.getA());
        float factor = interpolation > 0.0F ? Math.min(interpolation, 1.0F) : 0.0F;
        float nextGreen = end.green.calculate(instance);
        float green = start.green.calculate(instance);
        float nextBlue = end.blue.calculate(instance);
        float blue = start.blue.calculate(instance);
        float nextAlpha = end.alpha.calculate(instance);
        float alpha = start.alpha.calculate(instance);
        return new float[]{red + (nextRed - red) * factor, green + (nextGreen - green) * factor,
                blue + (nextBlue - blue) * factor, alpha + (nextAlpha - alpha) * factor};
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        float[] calculated = color.gradient.map.isEmpty() ? colorField.calculate(instance)
                : getCalculatedColor(instance, color.gradient.list, color.interpolant.calculate(instance));
        for (int component = 0; component < calculated.length; component++) {
            Float current = instance.getColorComponent(component);
            if (current != null) calculated[component] *= current;
        }
        instance.setColor(calculated[0], calculated[1], calculated[2], calculated[3]);
    }

    @Override
    public String toString() {
        return "ParticleAppearanceTinting{" +
                "color=" + color +
                ", colorField=" + colorField +
                '}';
    }

    public record Color(Gradient gradient, FloatMolangExp interpolant) {
        public static final Color EMPTY = new Color(Gradient.EMPTY, FloatMolangExp.ZERO);
        public static final Codec<Color> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Gradient.CODEC.lenientOptionalFieldOf("gradient", Gradient.EMPTY).forGetter(Color::gradient),
                FloatMolangExp.CODEC.lenientOptionalFieldOf("interpolant", FloatMolangExp.ZERO).forGetter(Color::interpolant)
        ).apply(instance, Color::new));

        @Override
        public String toString() {
            return "Color{" +
                    "gradient=" + gradient +
                    ", interpolant=" + interpolant +
                    '}';
        }

        public static final class Gradient {
            public static final Gradient EMPTY = new Gradient(Map.of());
            public static final Codec<Gradient> CODEC = Codec.either(Codec.list(ColorField.CODEC), Codec.unboundedMap(Codec.STRING, ColorField.CODEC)).xmap(
                    either -> either.map(l -> {
                        float step = l.size() > 1 ? 1.0F / (l.size() - 1) : 0.0F;
                        ImmutableMap.Builder<String, ColorField> builder = ImmutableMap.builder();
                        for (int i = 0; i < l.size(); i++) builder.put(Float.toString(i * step), l.get(i));
                        return new Gradient(builder.build());
                    }, Gradient::new),
                    gradient -> Either.right(gradient.map)
            );
            public final Map<String, ColorField> map;

            public final float range;
            public final ArrayList<Tuple<Float, ColorField>> list;

            public Gradient(Map<String, ColorField> map) {
                this.map = map;
                this.list = new ArrayList<>();
                map.entrySet().stream()
                        .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                        .sorted(Comparator.comparing(Tuple::getA))
                        .forEachOrdered(list::add);
                this.range = list.isEmpty() ? 0.0F : list.getLast().getA();
            }

            @Override
            public String toString() {
                return "Gradient{" +
                        "map=" + map +
                        '}';
            }
        }
    }

    public record ColorField(FloatMolangExp red, FloatMolangExp green, FloatMolangExp blue, FloatMolangExp alpha) {
        public static final ColorField EMPTY = new ColorField(FloatMolangExp.ONE, FloatMolangExp.ONE, FloatMolangExp.ONE, FloatMolangExp.ONE);
        private static final Codec<FloatMolangExp> CHANNEL_CODEC = FloatMolangExp.CODEC.orElse(
                (java.util.function.Consumer<String>) error -> BedrockParticles.LOGGER.warn("Invalid particle color component: {}", error),
                FloatMolangExp.ONE);
        public static final Codec<ColorField> CODEC = Codec.either(Codec.STRING, Codec.list(CHANNEL_CODEC, 3, 4)).xmap(
                either -> either.map(hex -> {
                    hex = hex.replace("#", "");
                    if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("The size is not allowed");
                    float a, r, g, b;
                    float v0 = Integer.parseInt(hex.substring(0, 2), 16) / 255.0F;
                    float v1 = Integer.parseInt(hex.substring(2, 4), 16) / 255.0F;
                    float v2 = Integer.parseInt(hex.substring(4, 6), 16) / 255.0F;
                    if (hex.length() == 6) {
                        a = 0.0F;
                        r = v0;
                        g = v1;
                        b = v2;
                    } else {
                        a = v0;
                        r = v1;
                        g = v2;
                        b = Integer.parseInt(hex.substring(6, 8), 16) / 255.0F;
                    }
                    return new ColorField(FloatMolangExp.ofConstant(r), FloatMolangExp.ofConstant(g), FloatMolangExp.ofConstant(b), FloatMolangExp.ofConstant(a));
                }, exps -> new ColorField(exps.getFirst(), exps.get(1), exps.get(2), exps.size() == 4 ? exps.get(3) : FloatMolangExp.ONE)),
                field -> Either.right(List.of(field.red, field.green, field.blue, field.alpha))
        );

        public float[] calculate(MolangInstance instance) {
            return new float[]{red.calculate(instance), green.calculate(instance), blue.calculate(instance), alpha.calculate(instance)};
        }



        @Override
        public String toString() {
            return "ColorField{" +
                    "red=" + red +
                    ", green=" + green +
                    ", blue=" + blue +
                    ", alpha=" + alpha +
                    '}';
        }
    }
}