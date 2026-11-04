package com.kltyton.bedrockruntime.examples;

import com.kltyton.bedrockruntime.api.model.EnhancedModels;

public final class ExampleResources {
    private static final String HUMANOID = "kltyton_bedrock_runtime_examples:geckolib/models/entity/kltyton_bedrock_runtime/example_humanoid.geo.json";
    private static final String TREE = "kltyton_bedrock_runtime_examples:geckolib/models/entity/kltyton_bedrock_runtime/example_tree.geo.json";
    private static final String BAKED = "kltyton_bedrock_runtime_examples:geckolib/models/entity/kltyton_bedrock_runtime/example_baked.geo.json";

    private ExampleResources() {
    }

    public static void register(Class<?> owner) {
        EnhancedModels.registerResource(HUMANOID, owner);
        EnhancedModels.registerResource(TREE, owner);
        EnhancedModels.registerResource(BAKED, owner);
    }

    public static String humanoid() {
        return HUMANOID;
    }

    public static String tree() {
        return TREE;
    }

    public static String baked() {
        return BAKED;
    }
}
