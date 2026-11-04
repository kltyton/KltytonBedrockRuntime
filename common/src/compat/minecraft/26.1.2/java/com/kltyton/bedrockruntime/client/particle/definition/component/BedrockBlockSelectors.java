package com.kltyton.bedrockruntime.client.particle.definition.component;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
        Set<Identifier> names = new HashSet<>();
        for (String selector : selectors) {
            Identifier name = Identifier.tryParse(selector);
            if (name == null) throw new IllegalArgumentException("Invalid particle block name: " + selector);
            names.add(name);
        }
        Set<BlockState> result = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier javaName = BuiltInRegistries.BLOCK.getKey(block);
            if (javaName == null) continue;
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            List<String> mapped = javaName.getNamespace().equals("minecraft") ? NAMES.get(javaName.getPath()) : null;
            if (mapped == null) {
                if (names.contains(javaName)) result.addAll(states);
                continue;
            }
            if (mapped.stream().noneMatch(name -> names.contains(Identifier.withDefaultNamespace(name)))) continue;
            if (mapped.size() != states.size()) {
                throw new IllegalStateException("Particle block state mapping does not match " + javaName);
            }
            for (int i = 0; i < states.size(); i++) {
                if (names.contains(Identifier.withDefaultNamespace(mapped.get(i)))) result.add(states.get(i));
            }
        }
        return result;
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
