package com.kltyton.bedrockruntime.client.particle.definition.component;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves Bedrock block identifiers used by particle expiration components. */
final class BedrockBlockSelectors {
    private static final String RESOURCE = "/assets/" + com.kltyton.bedrockruntime.particle.BedrockParticles.MODID + "/bedrock/block_names.tsv";
    private static final Map<String, List<String>> NAMES = loadNames();

    private BedrockBlockSelectors() {}

    static Set<BlockState> resolve(Set<String> selectors) {
        Set<ResourceLocation> names = new HashSet<>();
        for (String selector : selectors) {
            ResourceLocation name = ResourceLocation.tryParse(selector);
            if (name == null) throw new IllegalArgumentException("Invalid particle block name: " + selector);
            names.add(name);
        }
        Set<BlockState> result = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation javaName = BuiltInRegistries.BLOCK.getKey(block);
            if (javaName == null) continue;
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            List<String> mapped = javaName.getNamespace().equals("minecraft") ? NAMES.get(javaName.getPath()) : null;
            if (mapped == null) {
                if (names.contains(javaName)) result.addAll(states);
                continue;
            }
            if (mapped.isEmpty()) continue;
            if (mapped.stream().noneMatch(name -> names.contains(new ResourceLocation("minecraft", name)))) continue;
            for (BlockState state : states) {
                String resolved = mappedName(block, state, mapped);
                if (resolved != null && names.contains(new ResourceLocation("minecraft", resolved))) {
                    result.add(state);
                }
            }
        }
        return result;
    }

    private static boolean booleanProperty(BlockState state, String name) {
        for (var property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && property.getName().equals(name)) {
                return state.getValue(bool);
            }
        }
        return false;
    }

    private static @Nullable String mappedName(Block block, BlockState state, List<String> mapped) {
        String first = mapped.get(0);
        if (mapped.stream().allMatch(first::equals)) return first;
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (path.equals("water") || path.equals("lava")) {
            boolean source = state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) == 0;
            if (source) return first;
            for (String name : mapped) if (name.contains("flowing")) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.SlabBlock) {
            boolean doubleSlab = state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)
                    == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE;
            for (String name : mapped) {
                if (name.contains("double") == doubleSlab) return name;
            }
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
            boolean lit = booleanProperty(state, "lit");
            for (String name : mapped) if (name.startsWith("lit_") == lit) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.RedStoneOreBlock) {
            boolean lit = booleanProperty(state, "lit");
            for (String name : mapped) if (name.startsWith("lit_") == lit) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.RedstoneTorchBlock) {
            boolean lit = booleanProperty(state, "lit");
            for (String name : mapped) if (name.contains("unlit") != lit) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.DiodeBlock) {
            boolean powered = booleanProperty(state, "powered");
            for (String name : mapped) if (name.startsWith("powered_") == powered) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.RedstoneLampBlock) {
            boolean lit = booleanProperty(state, "lit");
            for (String name : mapped) if (name.startsWith("lit_") == lit) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.DaylightDetectorBlock) {
            boolean inverted = booleanProperty(state, "inverted");
            for (String name : mapped) if (name.contains("inverted") == inverted) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.LightBlock) {
            int level = state.getValue(net.minecraft.world.level.block.LightBlock.LEVEL);
            String expected = "light_block_" + level;
            for (String name : mapped) if (name.equals(expected)) return name;
            return null;
        }
        if (block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock) {
            boolean sticky = state.getValue(net.minecraft.world.level.block.piston.PistonHeadBlock.TYPE)
                    == net.minecraft.world.level.block.state.properties.PistonType.STICKY;
            for (String name : mapped) if (name.contains("sticky") == sticky) return name;
            return null;
        }
        if (path.equals("cave_vines") || path.equals("cave_vines_plant")) {
            boolean berries = state.getValue(net.minecraft.world.level.block.CaveVines.BERRIES);
            for (String name : mapped) {
                if (path.equals("cave_vines")) {
                    if (berries ? name.contains("head_with_berries") : name.equals("cave_vines")) return name;
                } else if (berries ? name.contains("body_with_berries") : name.equals("cave_vines")) {
                    return name;
                }
            }
            return null;
        }
        com.kltyton.bedrockruntime.particle.BedrockParticles.LOGGER.warn(
                "Skipping untyped multi-state particle block name mapping for {}", path);
        return null;
    }

    private static Map<String, List<String>> loadNames() {
        InputStream input = BedrockBlockSelectors.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing particle block name mapping: " + RESOURCE);
        Map<String, List<String>> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] entry = line.split("\\t", 2);
                if (entry.length != 2) throw new IllegalStateException("Invalid particle block name mapping: " + line);
                List<String> names = new ArrayList<>();
                for (String run : entry[1].split(",")) {
                    int separator = run.lastIndexOf('*');
                    if (separator <= 0) throw new IllegalStateException("Invalid particle block name mapping: " + line);
                    String name = run.substring(0, separator);
                    int count = Integer.parseInt(run.substring(separator + 1));
                    for (int i = 0; i < count; i++) names.add(name);
                }
                if (result.put(entry[0], List.copyOf(names)) != null) {
                    throw new IllegalStateException("Duplicate particle block name mapping: " + entry[0]);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read particle block name mapping: " + RESOURCE, exception);
        }
        return Map.copyOf(result);
    }
}
