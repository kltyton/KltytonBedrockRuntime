package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

/** Tests biome identifiers or Java biome tags at the attached entity's position. */
public final class EntityBiomeQuery extends MathFunction {
    public enum Kind { ALL_TAGS, ANY_TAGS, ANY_IDENTIFIER }

    private final Kind kind;
    private final MathValue[] arguments;

    public EntityBiomeQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        if (arguments.length == 0) {
            throw new IllegalArgumentException("Entity biome query requires at least one string argument");
        }
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        Entity entity = instance.getAttachedEntity();
        if (entity == null) return 0.0;

        BlockPos position = entity.blockPosition();
        if (!entity.level().isInWorldBounds(position) || !entity.level().isLoaded(position)) {
            return 0.0;
        }

        Holder<Biome> biome = entity.level().getBiome(position);
        Optional<net.minecraft.resources.ResourceKey<Biome>> biomeKey = biome.unwrapKey();
        if (kind == Kind.ANY_IDENTIFIER && biomeKey.isEmpty()) return 0.0;

        boolean all = kind == Kind.ALL_TAGS;
        for (MathValue argument : arguments) {
            String name = argument.getString(instance);
            if (name == null) {
                throw new IllegalArgumentException(getName() + " requires string arguments");
            }

            ResourceLocation identifier = ResourceLocation.parse(name);
            boolean matches = kind == Kind.ANY_IDENTIFIER
                    ? biomeKey.orElseThrow().location().equals(identifier)
                    : biome.is(TagKey.create(Registries.BIOME, identifier));
            if (all && !matches) return 0.0;
            if (!all && matches) return 1.0;
        }
        return all ? 1.0 : 0.0;
    }

    @Override public String getName() {
        if (kind == null) return "query.entity_biome";
        return switch (kind) {
            case ALL_TAGS -> "query.entity_biome_has_all_tags";
            case ANY_TAGS -> "query.entity_biome_has_any_tags";
            case ANY_IDENTIFIER -> "query.entity_biome_has_any_identifier";
        };
    }
    @Override public int getMinArgs() { return 1; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
