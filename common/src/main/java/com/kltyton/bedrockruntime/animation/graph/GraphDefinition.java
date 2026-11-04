package com.kltyton.bedrockruntime.animation.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable animation graph definition.
 *
 * <p>Nodes are referenced by stable string ids. The graph is validated on construction for
 * missing references and cycles. All lists and maps are defensive copies.</p>
 */
public final class GraphDefinition {
    public enum TimeMode {
        ABSOLUTE,
        LOOP,
        PHASE
    }

    /**
     * POSE nodes produce local poses. VELOCITY nodes produce velocity poses used only by Hermite
     * transition nodes; their translation is pixels/second, rotation is parent-space angular
     * velocity in radians/second, and scale is ratio/second.
     */
    public enum ValueKind {
        POSE,
        VELOCITY
    }

    public sealed interface Node permits AnimationNode, Blend1DNode, Blend2DNode, AdditiveNode, MaskNode,
            BlendNode, HermiteNode, VelocityInputNode {
        String id();

        List<String> references();

        default ValueKind kind() {
            return ValueKind.POSE;
        }
    }

    public record Blend1DEntry(float position, String node) {
        public Blend1DEntry {
            if (!Float.isFinite(position)) {
                throw new IllegalArgumentException("Blend1D entry position must be finite");
            }
            position = position == 0.0f ? 0.0f : position;
            node = GraphDefinition.requireId(node, "Blend1D entry node");
        }
    }

    public record Blend2DEntry(float x, float y, String node) {
        public Blend2DEntry {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Blend2D entry coordinates must be finite");
            }
            x = x == 0.0f ? 0.0f : x;
            y = y == 0.0f ? 0.0f : y;
            node = GraphDefinition.requireId(node, "Blend2D entry node");
        }
    }

    public record AnimationNode(
            String id,
            String animation,
            TimeMode timeMode,
            double speed,
            double offset,
            double duration,
            boolean loop) implements Node {
        public AnimationNode {
            id = GraphDefinition.requireId(id, "node id");
            animation = GraphDefinition.requireId(animation, "animation");
            timeMode = Objects.requireNonNull(timeMode, "timeMode");
            GraphDefinition.requireFinite(speed, "speed");
            GraphDefinition.requireFinite(offset, "offset");
            GraphDefinition.requireFinite(duration, "duration");
            if (duration < 0.0) {
                throw new IllegalArgumentException("Animation duration must not be negative");
            }
        }

        public AnimationNode(String id, String animation) {
            this(id, animation, TimeMode.LOOP, 1.0, 0.0, 0.0, true);
        }

        @Override
        public List<String> references() {
            return List.of();
        }

        public double localSeconds(double seconds) {
            return localSeconds(seconds, duration);
        }

        public double localSeconds(double seconds, double clipDuration) {
            GraphDefinition.requireFinite(seconds, "seconds");
            GraphDefinition.requireFinite(clipDuration, "clip duration");
            double value = offset + seconds * speed;
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Animation local time overflow for node '" + id + "'");
            }
            if (timeMode == TimeMode.LOOP) {
                return duration > 0.0 ? wrap(value, duration) : value;
            }
            if (timeMode == TimeMode.PHASE) {
                double resolvedDuration = clipDuration > 0.0 ? clipDuration : duration;
                if (resolvedDuration <= 0.0) {
                    throw new IllegalArgumentException("Phase sampling requires a positive clip duration for node '"
                            + id + "' (" + animation + ")");
                }
                double phase = value - Math.floor(value);
                return phase * resolvedDuration;
            }
            return value;
        }
    }

    /**
     * Caller-supplied velocity pose, addressed by {@link #inputKey()}.
     *
     * <p>Translation is pixels per second, rotation is parent-space angular velocity in radians per
     * second, and scale is ratio per second. Channel flags are sparse; an absent channel means zero
     * velocity. Values are supplied through {@code GraphRuntime.setVelocityInput}.</p>
     */
    public record VelocityInputNode(String id, String inputKey) implements Node {
        public VelocityInputNode {
            id = GraphDefinition.requireId(id, "node id");
            inputKey = GraphDefinition.requireId(inputKey, "velocity input key");
        }

        @Override
        public List<String> references() {
            return List.of();
        }

        @Override
        public ValueKind kind() {
            return ValueKind.VELOCITY;
        }
    }

    public record Blend1DNode(String id, String parameter, List<Blend1DEntry> entries) implements Node {
        public Blend1DNode {
            id = GraphDefinition.requireId(id, "node id");
            parameter = GraphDefinition.requireId(parameter, "parameter");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Blend1D node '" + id + "' requires at least one entry");
            }
            Set<Integer> positions = new HashSet<>();
            for (Blend1DEntry entry : entries) {
                Objects.requireNonNull(entry, "entry");
                if (!positions.add(Float.floatToIntBits(entry.position()))) {
                    throw new IllegalArgumentException("Blend1D node '" + id
                            + "' contains duplicate position " + entry.position());
                }
            }
        }

        @Override
        public List<String> references() {
            List<String> result = new ArrayList<>(entries.size());
            for (Blend1DEntry entry : entries) {
                result.add(entry.node());
            }
            return result;
        }
    }

    public record Blend2DNode(
            String id,
            String parameterX,
            String parameterY,
            List<Blend2DEntry> entries) implements Node {
        public Blend2DNode {
            id = GraphDefinition.requireId(id, "node id");
            parameterX = GraphDefinition.requireId(parameterX, "parameterX");
            parameterY = GraphDefinition.requireId(parameterY, "parameterY");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Blend2D node '" + id + "' requires at least one entry");
            }
            Set<Long> positions = new HashSet<>();
            for (Blend2DEntry entry : entries) {
                Objects.requireNonNull(entry, "entry");
                long key = ((long) Float.floatToIntBits(entry.x()) << 32)
                        ^ (Float.floatToIntBits(entry.y()) & 0xffffffffL);
                if (!positions.add(key)) {
                    throw new IllegalArgumentException("Blend2D node '" + id
                            + "' contains duplicate coordinate (" + entry.x() + ", " + entry.y() + ")");
                }
            }
        }

        @Override
        public List<String> references() {
            List<String> result = new ArrayList<>(entries.size());
            for (Blend2DEntry entry : entries) {
                result.add(entry.node());
            }
            return result;
        }
    }

    public record AdditiveNode(
            String id,
            String base,
            String additive,
            double weight,
            String weightParameter) implements Node {
        public AdditiveNode {
            id = GraphDefinition.requireId(id, "node id");
            base = GraphDefinition.requireId(base, "base");
            additive = GraphDefinition.requireId(additive, "additive");
            GraphDefinition.requireFinite(weight, "weight");
            if (weightParameter != null && weightParameter.isBlank()) {
                throw new IllegalArgumentException("Additive weightParameter must not be blank");
            }
        }

        @Override
        public List<String> references() {
            return List.of(base, additive);
        }
    }

    public record BlendNode(
            String id,
            String base,
            String input,
            double weight,
            String weightParameter) implements Node {
        public BlendNode {
            id = GraphDefinition.requireId(id, "node id");
            base = GraphDefinition.requireId(base, "base");
            input = GraphDefinition.requireId(input, "input");
            GraphDefinition.requireFinite(weight, "weight");
            if (weightParameter != null && weightParameter.isBlank()) {
                throw new IllegalArgumentException("Blend weightParameter must not be blank");
            }
        }

        @Override
        public List<String> references() {
            return List.of(base, input);
        }
    }

    public record HermiteNode(
            String id,
            String from,
            String to,
            String fromVelocity,
            String toVelocity,
            double duration,
            String phaseParameter,
            double phase) implements Node {
        public HermiteNode {
            id = GraphDefinition.requireId(id, "node id");
            from = GraphDefinition.requireId(from, "from");
            to = GraphDefinition.requireId(to, "to");
            fromVelocity = GraphDefinition.requireOptionalId(fromVelocity, "fromVelocity");
            toVelocity = GraphDefinition.requireOptionalId(toVelocity, "toVelocity");
            GraphDefinition.requireFinite(duration, "duration");
            GraphDefinition.requireFinite(phase, "phase");
            if (duration < 0.0) {
                throw new IllegalArgumentException("Hermite duration must not be negative");
            }
            if (phaseParameter != null && phaseParameter.isBlank()) {
                throw new IllegalArgumentException("Hermite phaseParameter must not be blank");
            }
        }

        public HermiteNode(String id, String from, String to, double duration, double phase) {
            this(id, from, to, null, null, duration, null, phase);
        }

        @Override
        public List<String> references() {
            List<String> result = new ArrayList<>(4);
            result.add(from);
            result.add(to);
            if (fromVelocity != null) {
                result.add(fromVelocity);
            }
            if (toVelocity != null) {
                result.add(toVelocity);
            }
            return List.copyOf(result);
        }
    }

    public record MaskNode(
            String id,
            String node,
            float weight,
            List<String> names,
            List<Integer> indices,
            List<String> subtrees) implements Node {
        public MaskNode {
            id = GraphDefinition.requireId(id, "node id");
            node = GraphDefinition.requireId(node, "node");
            if (!Float.isFinite(weight) || weight < 0.0f || weight > 1.0f) {
                throw new IllegalArgumentException("Mask weight must be finite and within [0, 1]");
            }
            names = List.copyOf(Objects.requireNonNull(names, "names"));
            indices = List.copyOf(Objects.requireNonNull(indices, "indices"));
            subtrees = List.copyOf(Objects.requireNonNull(subtrees, "subtrees"));
            for (String name : names) {
                GraphDefinition.requireId(name, "mask bone name");
            }
            for (String subtree : subtrees) {
                GraphDefinition.requireId(subtree, "mask subtree name");
            }
            for (Integer index : indices) {
                if (index == null || index < 0) {
                    throw new IllegalArgumentException("Mask bone indices must be non-negative");
                }
            }
            if (names.isEmpty() && indices.isEmpty() && subtrees.isEmpty()) {
                throw new IllegalArgumentException("Mask node '" + id + "' requires at least one target");
            }
        }

        @Override
        public List<String> references() {
            return List.of(node);
        }
    }

    private final Map<String, Node> nodes;
    private final List<Node> nodeList;
    private final String output;
    private final Map<String, Double> defaultParameters;

    public GraphDefinition(Map<String, Node> nodes, String output, Map<String, Double> defaultParameters) {
        Objects.requireNonNull(nodes, "nodes");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Animation graph requires at least one node");
        }
        LinkedHashMap<String, Node> copiedNodes = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String key = GraphDefinition.requireId(entry.getKey(), "node id");
            Node node = Objects.requireNonNull(entry.getValue(), "node '" + key + "'");
            if (!key.equals(node.id())) {
                throw new IllegalArgumentException("Node map key '" + key
                        + "' does not match node id '" + node.id() + "'");
            }
            copiedNodes.put(key, node);
        }
        this.nodes = Collections.unmodifiableMap(copiedNodes);
        this.nodeList = List.copyOf(copiedNodes.values());
        this.output = GraphDefinition.requireId(output, "output");
        if (!copiedNodes.containsKey(this.output)) {
            throw new IllegalArgumentException("Animation graph output node '" + this.output
                    + "' does not exist");
        }
        LinkedHashMap<String, Double> copiedDefaults = new LinkedHashMap<>();
        if (defaultParameters != null) {
            for (Map.Entry<String, Double> entry : defaultParameters.entrySet()) {
                String name = GraphDefinition.requireId(entry.getKey(), "parameter name");
                Double value = Objects.requireNonNull(entry.getValue(), "parameter '" + name + "'");
                GraphDefinition.requireFinite(value, "default parameter '" + name + "'");
                copiedDefaults.put(name, value);
            }
        }
        this.defaultParameters = Collections.unmodifiableMap(copiedDefaults);
        for (Node node : nodeList) {
            for (String reference : node.references()) {
                if (!copiedNodes.containsKey(reference)) {
                    throw new IllegalArgumentException("Animation graph node '" + node.id()
                            + "' references missing node '" + reference + "'");
                }
            }
        }
        validateValueKinds(copiedNodes, this.output);
        detectCycles(copiedNodes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Node node(String id) {
        Node node = nodes.get(Objects.requireNonNull(id, "id"));
        if (node == null) {
            throw new IllegalArgumentException("Unknown animation graph node '" + id + "'");
        }
        return node;
    }

    public boolean hasNode(String id) {
        return id != null && nodes.containsKey(id);
    }

    public Collection<Node> nodes() {
        return nodeList;
    }

    public String output() {
        return output;
    }

    public Map<String, Double> defaultParameters() {
        return defaultParameters;
    }

    public double defaultParameter(String name) {
        Double value = defaultParameters.get(name);
        return value == null ? 0.0 : value;
    }

    private static void validateValueKinds(Map<String, Node> nodes, String output) {
        Node outputNode = nodes.get(output);
        if (outputNode.kind() != ValueKind.POSE) {
            throw new IllegalArgumentException("Animation graph output node '" + output
                    + "' must be a pose node");
        }
        for (Node node : nodes.values()) {
            if (node instanceof AnimationNode || node instanceof VelocityInputNode) {
                continue;
            }
            if (node instanceof Blend1DNode blend) {
                for (int i = 0; i < blend.entries().size(); i++) {
                    requireNodeKind(nodes, blend.entries().get(i).node(), ValueKind.POSE,
                            node.id(), "entries[" + i + "].node");
                }
                continue;
            }
            if (node instanceof Blend2DNode blend) {
                for (int i = 0; i < blend.entries().size(); i++) {
                    requireNodeKind(nodes, blend.entries().get(i).node(), ValueKind.POSE,
                            node.id(), "entries[" + i + "].node");
                }
                continue;
            }
            if (node instanceof AdditiveNode additive) {
                requireNodeKind(nodes, additive.base(), ValueKind.POSE, node.id(), "base");
                requireNodeKind(nodes, additive.additive(), ValueKind.POSE, node.id(), "additive");
                continue;
            }
            if (node instanceof BlendNode blend) {
                requireNodeKind(nodes, blend.base(), ValueKind.POSE, node.id(), "base");
                requireNodeKind(nodes, blend.input(), ValueKind.POSE, node.id(), "input");
                continue;
            }
            if (node instanceof MaskNode mask) {
                requireNodeKind(nodes, mask.node(), ValueKind.POSE, node.id(), "node");
                continue;
            }
            if (node instanceof HermiteNode hermite) {
                requireNodeKind(nodes, hermite.from(), ValueKind.POSE, node.id(), "from");
                requireNodeKind(nodes, hermite.to(), ValueKind.POSE, node.id(), "to");
                if (hermite.fromVelocity() != null) {
                    requireNodeKind(nodes, hermite.fromVelocity(), ValueKind.VELOCITY,
                            node.id(), "fromVelocity");
                }
                if (hermite.toVelocity() != null) {
                    requireNodeKind(nodes, hermite.toVelocity(), ValueKind.VELOCITY,
                            node.id(), "toVelocity");
                }
            }
        }
    }

    private static void requireNodeKind(Map<String, Node> nodes, String reference, ValueKind expected,
            String ownerId, String field) {
        Node target = nodes.get(reference);
        if (target == null || target.kind() != expected) {
            throw new IllegalArgumentException("Animation graph node '" + ownerId + "' field '" + field
                    + "' requires a " + expected + " node but '" + reference + "' is "
                    + (target == null ? "missing" : target.kind()));
        }
    }

    private static void detectCycles(Map<String, Node> nodes) {
        Map<String, Integer> state = new HashMap<>();
        List<String> stack = new ArrayList<>();
        for (String id : nodes.keySet()) {
            visit(id, nodes, state, stack);
        }
    }

    private static void visit(String id, Map<String, Node> nodes, Map<String, Integer> state,
            List<String> stack) {
        int current = state.getOrDefault(id, 0);
        if (current == 2) {
            return;
        }
        if (current == 1) {
            int start = stack.indexOf(id);
            List<String> cycle = new ArrayList<>(stack.subList(start, stack.size()));
            cycle.add(id);
            throw new IllegalArgumentException("Cycle in animation graph: " + String.join(" -> ", cycle));
        }
        state.put(id, 1);
        stack.add(id);
        for (String reference : nodes.get(id).references()) {
            visit(reference, nodes, state, stack);
        }
        stack.remove(stack.size() - 1);
        state.put(id, 2);
    }

    static String requireId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    static String requireOptionalId(String value, String field) {
        return value == null ? null : requireId(value, field);
    }

    static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    private static double wrap(double value, double duration) {
        double wrapped = value % duration;
        if (wrapped < 0.0) {
            wrapped += duration;
        }
        return wrapped;
    }

    public static final class Builder {
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final Map<String, Double> defaultParameters = new LinkedHashMap<>();
        private String output = "root";

        private Builder() {
        }

        public Builder node(Node node) {
            Objects.requireNonNull(node, "node");
            nodes.put(node.id(), node);
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder defaultParameter(String name, double value) {
            defaultParameters.put(name, value);
            return this;
        }

        public GraphDefinition build() {
            return new GraphDefinition(nodes, output, defaultParameters);
        }
    }
}
