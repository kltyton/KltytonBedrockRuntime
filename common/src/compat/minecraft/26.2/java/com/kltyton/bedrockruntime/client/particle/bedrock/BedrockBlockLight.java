package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.kltyton.bedrockruntime.particle.BedrockParticles;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Projects vanilla light filtering onto Java states for particle height queries. */
public final class BedrockBlockLight {
    private static final String RESOURCE = "/assets/" + BedrockParticles.MODID + "/bedrock/block_height_filters.tsv";
    private static volatile Map<BlockState, Boolean> filters = Map.of();

    private BedrockBlockLight() {
    }

    public static void initialize() {
        Map<String, String> definitions = new HashMap<>();
        InputStream input = BedrockBlockLight.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing particle height filters: " + RESOURCE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] entry = line.split("\t", -1);
                if (entry.length != 2 || entry[1].isEmpty()
                        || entry[1].chars().anyMatch(value -> value != '0' && value != '1')
                        || definitions.putIfAbsent(entry[0], entry[1]) != null) {
                    throw new IllegalStateException("Invalid particle height filter: " + line);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read particle height filters: " + RESOURCE, exception);
        }
        Map<BlockState, Boolean> snapshot = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !id.getNamespace().equals("minecraft")) continue;
            String flags = definitions.get(id.getPath());
            if (flags == null) continue;
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            if (flags.length() != 1 && flags.length() != states.size()) {
                throw new IllegalStateException("Particle height filter states do not match " + id);
            }
            for (int i = 0; i < states.size(); i++) {
                snapshot.put(states.get(i), flags.charAt(flags.length() == 1 ? 0 : i) == '1');
            }
        }
        filters = java.util.Collections.unmodifiableMap(snapshot);
    }

    public static boolean stopsHeightmap(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) return true;
        var fluid = state.getFluidState();
        if (fluid.is(FluidTags.WATER) && fluid.isSource()) return true;
        Boolean filtered = filters.get(state);
        return filtered != null ? filtered : state.getLightDampening() > 0;
    }
}
