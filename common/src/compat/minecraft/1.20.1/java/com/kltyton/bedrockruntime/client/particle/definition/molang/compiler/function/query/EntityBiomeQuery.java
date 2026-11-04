package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Tests Bedrock biome identifiers and tags at the attached entity's position. */
public final class EntityBiomeQuery extends MathFunction {
    public enum Kind { ALL_TAGS, ANY_TAGS, ANY_IDENTIFIER }

    private static final java.util.Set<String> REPORTED_ERRORS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Kind kind;
    private final MathValue[] arguments;

    public EntityBiomeQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        if (arguments.length == 0) return contentError("requires at least one string argument");
        Entity entity = instance.getAttachedEntity();
        if (entity == null || entity.isRemoved()) return contentError("requires an entity");

        BlockPos position = BlockPos.containing(ActorQueryPosition.x(entity), ActorQueryPosition.y(entity), ActorQueryPosition.z(entity));
        if (entity.level().isOutsideBuildHeight(position) || !entity.level().isLoaded(position)) {
            return 0.0;
        }

        Holder<Biome> biome = entity.level().getBiome(position);
        Optional<net.minecraft.resources.ResourceKey<Biome>> biomeKey = biome.unwrapKey();
        if (kind == Kind.ANY_IDENTIFIER && biomeKey.isEmpty()) return 0.0;
        BedrockBiomeMapping.BedrockBiome bedrockBiome = biomeKey
                .map(key -> BedrockBiomeMapping.forJava(key.location())).orElse(null);

        boolean all = kind == Kind.ALL_TAGS;
        for (MathValue argument : arguments) {
            String name = argument.getString(instance);
            if (name == null) {
                return contentError("requires string arguments");
            }

            ResourceLocation identifier = ResourceLocation.tryParse(name);
            boolean matches = identifier != null && (kind == Kind.ANY_IDENTIFIER
                    ? biomeKey.orElseThrow().location().equals(identifier)
                            || bedrockBiome != null && bedrockBiome.matchesIdentifier(identifier)
                    : bedrockBiome != null && bedrockBiome.tags().contains(name)
                            || biome.is(TagKey.create(Registries.BIOME, identifier)));
            if (all && !matches) return 0.0;
            if (!all && matches) return 1.0;
        }
        return all ? 1.0 : 0.0;
    }

    private double contentError(String message) {
        if (REPORTED_ERRORS.add(kind + ":" + message)) {
            BedrockParticles.LOGGER.warn("{}: {}", getName(), message);
        }
        return 0.0;
    }

    @Override public String getName() {
        if (kind == null) return "query.entity_biome";
        return switch (kind) {
            case ALL_TAGS -> "query.entity_biome_has_all_tags";
            case ANY_TAGS -> "query.entity_biome_has_any_tags";
            case ANY_IDENTIFIER -> "query.entity_biome_has_any_identifier";
        };
    }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
