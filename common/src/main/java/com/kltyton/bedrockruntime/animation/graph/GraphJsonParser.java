package com.kltyton.bedrockruntime.animation.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Minimal animation graph JSON v1 parser.
 *
 * <p>Supported node types are {@code animation}, {@code blend1d}, {@code blend2d},
 * {@code blend}, {@code additive}, {@code hermite}, {@code mask} and {@code velocity_input}.
 * Errors carry a JSON path such as {@code $.nodes.locomotion.entries[1].node}.</p>
 */
public final class GraphJsonParser {
    private GraphJsonParser() {
    }

    public static GraphDefinition parse(String json) {
        if (json == null) {
            throw new GraphParseException("$", "JSON input must not be null");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException exception) {
            throw new GraphParseException("$", message(exception), exception);
        }
        return parse(root);
    }

    public static GraphDefinition parse(JsonElement element) {
        JsonObject root = requireObject(element, "$");
        readVersion(root);
        Map<String, Double> defaultParameters = readParameters(root);
        String output = optionalString(root, "output", "root", "$.output");
        JsonObject nodesObject = requireObject(present(root, "nodes"), "$.nodes");
        if (nodesObject.size() == 0) {
            throw new GraphParseException("$.nodes", "must contain at least one node");
        }
        Map<String, GraphDefinition.Node> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : nodesObject.entrySet()) {
            String id = entry.getKey();
            String nodePath = "$.nodes." + id;
            if (id == null || id.isBlank()) {
                throw new GraphParseException(nodePath, "node id must not be blank");
            }
            JsonObject nodeObject = requireObject(entry.getValue(), nodePath);
            GraphDefinition.Node node = parseNode(id, nodeObject, nodePath, nodesObject);
            if (nodes.put(id, node) != null) {
                throw new GraphParseException(nodePath, "duplicate node id");
            }
        }
        if (!nodesObject.has(output) || nodesObject.get(output).isJsonNull()) {
            throw new GraphParseException("$.output", "unknown output node '" + output + "'");
        }
        validateReferenceKinds(nodes, output);
        detectCycles(nodes);
        try {
            return new GraphDefinition(nodes, output, defaultParameters);
        } catch (IllegalArgumentException exception) {
            throw new GraphParseException("$", message(exception), exception);
        }
    }

    private static GraphDefinition.Node parseNode(String id, JsonObject object, String path,
            JsonObject allNodes) {
        try {
            String type = requireString(object, "type", path + ".type").toLowerCase(Locale.ROOT);
            switch (type) {
                case "animation":
                    return parseAnimation(id, object, path);
                case "blend1d":
                    return parseBlend1D(id, object, path, allNodes);
                case "blend2d":
                    return parseBlend2D(id, object, path, allNodes);
                case "additive":
                    return parseAdditive(id, object, path, allNodes);
                case "blend":
                    return parseBlend(id, object, path, allNodes);
                case "hermite":
                case "transition":
                    return parseHermite(id, object, path, allNodes);
                case "mask":
                    return parseMask(id, object, path, allNodes);
                case "velocity_input":
                    return parseVelocityInput(id, object, path);
                default:
                    throw new GraphParseException(path + ".type", "unsupported node type '" + type + "'");
            }
        } catch (GraphParseException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new GraphParseException(path, message(exception), exception);
        }
    }

    private static GraphDefinition.AnimationNode parseAnimation(String id, JsonObject object, String path) {
        String animation = requireString(object, "animation", path + ".animation");
        String timeText = optionalString(object, "time", "loop", path + ".time");
        GraphDefinition.TimeMode timeMode;
        switch (timeText.toLowerCase(Locale.ROOT)) {
            case "absolute":
                timeMode = GraphDefinition.TimeMode.ABSOLUTE;
                break;
            case "loop":
                timeMode = GraphDefinition.TimeMode.LOOP;
                break;
            case "phase":
                timeMode = GraphDefinition.TimeMode.PHASE;
                break;
            default:
                throw new GraphParseException(path + ".time", "expected absolute, loop or phase");
        }
        double speed = optionalDouble(object, "speed", 1.0, path + ".speed");
        double offset = optionalDouble(object, "offset", 0.0, path + ".offset");
        double duration = optionalDouble(object, "duration", 0.0, path + ".duration");
        if (duration < 0.0) {
            throw new GraphParseException(path + ".duration", "must not be negative");
        }
        boolean loop = optionalBoolean(object, "loop", timeMode == GraphDefinition.TimeMode.LOOP, path + ".loop");
        return new GraphDefinition.AnimationNode(id, animation, timeMode, speed, offset, duration, loop);
    }

    private static GraphDefinition.VelocityInputNode parseVelocityInput(String id, JsonObject object, String path) {
        JsonElement inputElement = present(object, "input");
        JsonElement keyElement = present(object, "key");

        if (inputElement != null && keyElement != null) {
            throw new GraphParseException(path, "use exactly one of input or key");
        }
        if (inputElement != null) {
            return new GraphDefinition.VelocityInputNode(id,
                    requireString(object, "input", path + ".input"));
        }
        if (keyElement != null) {
            return new GraphDefinition.VelocityInputNode(id,
                    requireString(object, "key", path + ".key"));
        }

        throw new GraphParseException(path + ".input", "is required");
    }

    private static GraphDefinition.Blend1DNode parseBlend1D(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String parameter = requireString(object, "parameter", path + ".parameter");
        JsonArray entriesArray = requireArray(present(object, "entries"), path + ".entries");
        if (entriesArray.size() == 0) {
            throw new GraphParseException(path + ".entries", "must contain at least one entry");
        }
        List<GraphDefinition.Blend1DEntry> entries = new ArrayList<>(entriesArray.size());
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < entriesArray.size(); i++) {
            String entryPath = path + ".entries[" + i + "]";
            JsonObject entryObject = requireObject(entriesArray.get(i), entryPath);
            float position = finiteFloat(requireNumber(entryObject, "position", entryPath + ".position"),
                    entryPath + ".position");
            position = position == 0.0f ? 0.0f : position;
            if (!positions.add(Float.floatToIntBits(position))) {
                throw new GraphParseException(entryPath + ".position", "duplicate position " + position);
            }
            String node = requireString(entryObject, "node", entryPath + ".node");
            requireReference(allNodes, node, entryPath + ".node");
            entries.add(new GraphDefinition.Blend1DEntry(position, node));
        }
        return new GraphDefinition.Blend1DNode(id, parameter, entries);
    }

    private static GraphDefinition.Blend2DNode parseBlend2D(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String parameterX = requireString(object, "parameterX", path + ".parameterX");
        String parameterY = requireString(object, "parameterY", path + ".parameterY");
        JsonArray entriesArray = requireArray(present(object, "entries"), path + ".entries");
        if (entriesArray.size() == 0) {
            throw new GraphParseException(path + ".entries", "must contain at least one entry");
        }
        List<GraphDefinition.Blend2DEntry> entries = new ArrayList<>(entriesArray.size());
        Set<Long> positions = new HashSet<>();
        for (int i = 0; i < entriesArray.size(); i++) {
            String entryPath = path + ".entries[" + i + "]";
            JsonObject entryObject = requireObject(entriesArray.get(i), entryPath);
            float x = finiteFloat(requireNumber(entryObject, "x", entryPath + ".x"), entryPath + ".x");
            float y = finiteFloat(requireNumber(entryObject, "y", entryPath + ".y"), entryPath + ".y");
            x = x == 0.0f ? 0.0f : x;
            y = y == 0.0f ? 0.0f : y;
            long key = ((long) Float.floatToIntBits(x) << 32) ^ (Float.floatToIntBits(y) & 0xffffffffL);
            if (!positions.add(key)) {
                throw new GraphParseException(entryPath, "duplicate coordinate (" + x + ", " + y + ")");
            }
            String node = requireString(entryObject, "node", entryPath + ".node");
            requireReference(allNodes, node, entryPath + ".node");
            entries.add(new GraphDefinition.Blend2DEntry(x, y, node));
        }
        return new GraphDefinition.Blend2DNode(id, parameterX, parameterY, entries);
    }

    private static GraphDefinition.AdditiveNode parseAdditive(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String base = requireString(object, "base", path + ".base");
        requireReference(allNodes, base, path + ".base");
        String additive = requireString(object, "additive", path + ".additive");
        requireReference(allNodes, additive, path + ".additive");
        double weight = optionalDouble(object, "weight", 1.0, path + ".weight");
        String weightParameter = optionalString(object, "weightParameter", null, path + ".weightParameter");
        return new GraphDefinition.AdditiveNode(id, base, additive, weight, weightParameter);
    }

    private static GraphDefinition.BlendNode parseBlend(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String base = requireString(object, "base", path + ".base");
        requireReference(allNodes, base, path + ".base");
        String input = requireString(object, "input", path + ".input");
        requireReference(allNodes, input, path + ".input");
        double weight = optionalDouble(object, "weight", 1.0, path + ".weight");
        String weightParameter = optionalString(object, "weightParameter", null, path + ".weightParameter");
        return new GraphDefinition.BlendNode(id, base, input, weight, weightParameter);
    }

    private static GraphDefinition.HermiteNode parseHermite(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String from = requireString(object, "from", path + ".from");
        requireReference(allNodes, from, path + ".from");
        String to = requireString(object, "to", path + ".to");
        requireReference(allNodes, to, path + ".to");
        String fromVelocity = optionalReference(object, "fromVelocity", path + ".fromVelocity", allNodes);
        String toVelocity = optionalReference(object, "toVelocity", path + ".toVelocity", allNodes);
        double duration = optionalDouble(object, "duration", 0.0, path + ".duration");
        if (duration < 0.0) {
            throw new GraphParseException(path + ".duration", "must not be negative");
        }
        double phase = optionalDouble(object, "phase", 0.0, path + ".phase");
        String phaseParameter = optionalString(object, "phaseParameter", null, path + ".phaseParameter");
        return new GraphDefinition.HermiteNode(id, from, to, fromVelocity, toVelocity,
                duration, phaseParameter, phase);
    }

    private static GraphDefinition.MaskNode parseMask(String id, JsonObject object, String path,
            JsonObject allNodes) {
        String node = requireString(object, "node", path + ".node");
        requireReference(allNodes, node, path + ".node");
        double weight = optionalDouble(object, "weight", 1.0, path + ".weight");
        if (weight < 0.0 || weight > 1.0) {
            throw new GraphParseException(path + ".weight", "must be within [0, 1]");
        }
        List<String> names = optionalStringList(object, "names", path + ".names");
        List<Integer> indices = optionalIntegerList(object, "indices", path + ".indices");
        List<String> subtrees = optionalStringList(object, "subtrees", path + ".subtrees");
        if (names.isEmpty() && indices.isEmpty() && subtrees.isEmpty()) {
            throw new GraphParseException(path, "mask requires names, indices or subtrees");
        }
        return new GraphDefinition.MaskNode(id, node, (float) weight, names, indices, subtrees);
    }

    private static void readVersion(JsonObject root) {
        JsonElement element = present(root, "version");
        if (element == null) {
            return;
        }
        double version = requireNumber(element, "$.version");
        if (version != 1.0) {
            throw new GraphParseException("$.version", "unsupported version " + version);
        }
    }

    private static Map<String, Double> readParameters(JsonObject root) {
        JsonElement element = present(root, "parameters");
        if (element == null) {
            return Map.of();
        }
        JsonObject parameters = requireObject(element, "$.parameters");
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : parameters.entrySet()) {
            String path = "$.parameters." + entry.getKey();
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new GraphParseException(path, "parameter name must not be blank");
            }
            result.put(entry.getKey(), requireNumber(entry.getValue(), path));
        }
        return result;
    }

    private static String optionalReference(JsonObject object, String name, String path,
            JsonObject allNodes) {
        if (present(object, name) == null) {
            return null;
        }
        String reference = requireString(object, name, path);
        requireReference(allNodes, reference, path);
        return reference;
    }

    private static void requireReference(JsonObject nodes, String reference, String path) {
        if (!nodes.has(reference) || nodes.get(reference).isJsonNull()) {
            throw new GraphParseException(path, "unknown node reference '" + reference + "'");
        }
    }

    private static void validateReferenceKinds(Map<String, GraphDefinition.Node> nodes, String output) {
        GraphDefinition.Node outputNode = nodes.get(output);

        if (outputNode == null || outputNode.kind() != GraphDefinition.ValueKind.POSE) {
            throw new GraphParseException("$.output", "requires a pose node, but '" + output + "' is "
                    + (outputNode == null ? "missing" : kindName(outputNode.kind())));
        }

        for (GraphDefinition.Node node : nodes.values()) {
            String path = "$.nodes." + node.id();

            if (node instanceof GraphDefinition.AnimationNode
                    || node instanceof GraphDefinition.VelocityInputNode) {
                continue;
            }
            if (node instanceof GraphDefinition.Blend1DNode blend) {
                for (int i = 0; i < blend.entries().size(); i++) {
                    requireNodeKind(nodes, blend.entries().get(i).node(), GraphDefinition.ValueKind.POSE,
                            path + ".entries[" + i + "].node");
                }
                continue;
            }
            if (node instanceof GraphDefinition.Blend2DNode blend) {
                for (int i = 0; i < blend.entries().size(); i++) {
                    requireNodeKind(nodes, blend.entries().get(i).node(), GraphDefinition.ValueKind.POSE,
                            path + ".entries[" + i + "].node");
                }
                continue;
            }
            if (node instanceof GraphDefinition.AdditiveNode additive) {
                requireNodeKind(nodes, additive.base(), GraphDefinition.ValueKind.POSE, path + ".base");
                requireNodeKind(nodes, additive.additive(), GraphDefinition.ValueKind.POSE, path + ".additive");
                continue;
            }
            if (node instanceof GraphDefinition.BlendNode blend) {
                requireNodeKind(nodes, blend.base(), GraphDefinition.ValueKind.POSE, path + ".base");
                requireNodeKind(nodes, blend.input(), GraphDefinition.ValueKind.POSE, path + ".input");
                continue;
            }
            if (node instanceof GraphDefinition.MaskNode mask) {
                requireNodeKind(nodes, mask.node(), GraphDefinition.ValueKind.POSE, path + ".node");
                continue;
            }
            if (node instanceof GraphDefinition.HermiteNode hermite) {
                requireNodeKind(nodes, hermite.from(), GraphDefinition.ValueKind.POSE, path + ".from");
                requireNodeKind(nodes, hermite.to(), GraphDefinition.ValueKind.POSE, path + ".to");
                if (hermite.fromVelocity() != null) {
                    requireNodeKind(nodes, hermite.fromVelocity(), GraphDefinition.ValueKind.VELOCITY,
                            path + ".fromVelocity");
                }
                if (hermite.toVelocity() != null) {
                    requireNodeKind(nodes, hermite.toVelocity(), GraphDefinition.ValueKind.VELOCITY,
                            path + ".toVelocity");
                }
            }
        }
    }

    private static void requireNodeKind(Map<String, GraphDefinition.Node> nodes, String reference,
            GraphDefinition.ValueKind expected, String path) {
        GraphDefinition.Node node = nodes.get(reference);

        if (node == null) {
            throw new GraphParseException(path, "unknown node reference '" + reference + "'");
        }
        if (node.kind() != expected) {
            throw new GraphParseException(path, "requires a " + kindName(expected) + " node, but '"
                    + reference + "' is " + kindName(node.kind()));
        }
    }

    private static String kindName(GraphDefinition.ValueKind kind) {
        return kind == GraphDefinition.ValueKind.POSE ? "pose" : "velocity";
    }

    private static void detectCycles(Map<String, GraphDefinition.Node> nodes) {
        Map<String, Integer> state = new HashMap<>();
        List<String> stack = new ArrayList<>();
        for (String id : nodes.keySet()) {
            visit(id, nodes, state, stack);
        }
    }

    private static void visit(String id, Map<String, GraphDefinition.Node> nodes, Map<String, Integer> state,
            List<String> stack) {
        int current = state.getOrDefault(id, 0);
        if (current == 2) {
            return;
        }
        if (current == 1) {
            int start = stack.indexOf(id);
            List<String> cycle = new ArrayList<>(stack.subList(start, stack.size()));
            cycle.add(id);
            throw new GraphParseException("$.nodes." + id,
                    "cycle in graph: " + String.join(" -> ", cycle));
        }
        state.put(id, 1);
        stack.add(id);
        for (String reference : nodes.get(id).references()) {
            visit(reference, nodes, state, stack);
        }
        stack.remove(stack.size() - 1);
        state.put(id, 2);
    }

    private static JsonElement present(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() ? null : element;
    }

    private static JsonObject requireObject(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new GraphParseException(path, "must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonElement element, String path) {
        if (element == null || !element.isJsonArray()) {
            throw new GraphParseException(path, "must be an array");
        }
        return element.getAsJsonArray();
    }

    private static String requireString(JsonObject object, String name, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            throw new GraphParseException(path, "is required");
        }
        JsonPrimitive primitive = requirePrimitive(element, path);
        if (!primitive.isString()) {
            throw new GraphParseException(path, "must be a string");
        }
        String value = primitive.getAsString();
        if (value.isBlank()) {
            throw new GraphParseException(path, "must not be blank");
        }
        return value;
    }

    private static String optionalString(JsonObject object, String name, String defaultValue, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            return defaultValue;
        }
        JsonPrimitive primitive = requirePrimitive(element, path);
        if (!primitive.isString()) {
            throw new GraphParseException(path, "must be a string");
        }
        return primitive.getAsString();
    }

    private static double requireNumber(JsonObject object, String name, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            throw new GraphParseException(path, "is required");
        }
        return requireNumber(element, path);
    }

    private static double requireNumber(JsonElement element, String path) {
        JsonPrimitive primitive = requirePrimitive(element, path);
        if (!primitive.isNumber()) {
            throw new GraphParseException(path, "must be a number");
        }
        double value = primitive.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new GraphParseException(path, "must be finite");
        }
        return value;
    }

    private static double optionalDouble(JsonObject object, String name, double defaultValue, String path) {
        JsonElement element = present(object, name);
        return element == null ? defaultValue : requireNumber(element, path);
    }

    private static boolean optionalBoolean(JsonObject object, String name, boolean defaultValue, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            return defaultValue;
        }
        JsonPrimitive primitive = requirePrimitive(element, path);
        if (!primitive.isBoolean()) {
            throw new GraphParseException(path, "must be a boolean");
        }
        return primitive.getAsBoolean();
    }

    private static List<String> optionalStringList(JsonObject object, String name, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            return List.of();
        }
        JsonArray array = requireArray(element, path);
        List<String> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            String itemPath = path + "[" + i + "]";
            JsonPrimitive primitive = requirePrimitive(array.get(i), itemPath);
            if (!primitive.isString() || primitive.getAsString().isBlank()) {
                throw new GraphParseException(itemPath, "must be a non-blank string");
            }
            result.add(primitive.getAsString());
        }
        return List.copyOf(result);
    }

    private static List<Integer> optionalIntegerList(JsonObject object, String name, String path) {
        JsonElement element = present(object, name);
        if (element == null) {
            return List.of();
        }
        JsonArray array = requireArray(element, path);
        List<Integer> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            String itemPath = path + "[" + i + "]";
            double value = requireNumber(array.get(i), itemPath);
            if (value != Math.rint(value) || value < 0.0 || value > Integer.MAX_VALUE) {
                throw new GraphParseException(itemPath, "must be a non-negative integer");
            }
            result.add((int) value);
        }
        return List.copyOf(result);
    }

    private static JsonPrimitive requirePrimitive(JsonElement element, String path) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new GraphParseException(path, "must be a primitive value");
        }
        return element.getAsJsonPrimitive();
    }

    private static float finiteFloat(double value, String path) {
        float result = (float) value;
        if (!Float.isFinite(result)) {
            throw new GraphParseException(path, "is outside float range");
        }
        return result;
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
