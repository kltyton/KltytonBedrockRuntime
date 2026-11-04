package com.kltyton.bedrockruntime.client.particle.definition.component;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashSet;
import java.util.Set;

/** Resolves block names and optional property constraints before particle updates. */
final class BlockStateSelectors {
    private BlockStateSelectors() {}

    static Set<BlockState> resolve(Level level, Set<String> selectors) {
        return resolve(level, selectors, false);
    }

    static Set<BlockState> resolve(Level level, Set<String> selectors, boolean bedrockBlockNames) {
        HolderLookup<Block> lookup = level.holderLookup(Registries.BLOCK);
        Set<BlockState> resolved = new HashSet<>();
        if (bedrockBlockNames) {
            Set<String> names = new HashSet<>();
            for (String selector : selectors) {
                if (selector.indexOf('[') < 0) names.add(selector);
            }
            if (!names.isEmpty()) resolved.addAll(BedrockBlockSelectors.resolve(names));
        }
        for (String selector : selectors) {
            if (bedrockBlockNames && selector.indexOf('[') < 0) continue;
            BlockStateParser.BlockResult parsed;
            StringReader reader = new StringReader(selector);
            try {
                parsed = BlockStateParser.parseForBlock(lookup, reader, false);
            } catch (CommandSyntaxException exception) {
                throw new IllegalArgumentException("Invalid particle block selector: " + selector, exception);
            }
            reader.skipWhitespace();
            if (reader.canRead()) {
                throw new IllegalArgumentException("Invalid particle block selector: " + selector);
            }
            for (BlockState state : parsed.blockState().getBlock().getStateDefinition().getPossibleStates()) {
                boolean matches = true;
                for (var property : parsed.properties().entrySet()) {
                    if (!matches(state, property.getKey(), property.getValue())) {
                        matches = false;
                        break;
                    }
                }
                if (matches) resolved.add(state);
            }
        }
        return resolved;
    }

    private static <T extends Comparable<T>> boolean matches(BlockState state, Property<T> property, Comparable<?> expected) {
        return state.getValue(property).equals(expected);
    }
}
