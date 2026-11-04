package com.kltyton.bedrockruntime.examples;

import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphJsonParser;

import java.util.EnumMap;
import java.util.Map;

public enum ExampleScene {
    SPEED_1D("speed_1d", "1D speed blend: idle / walk / run", "idle", null),
    AIM_2D("aim_2d", "2D direction and aim blend", "idle", null),
    LEGS_AIM("legs_aim", "Leg locomotion with masked upper-body aim", "walk", null),
    ADDITIVE("additive", "Native additive controller collaboration", "idle", null),
    HERMITE("hermite", "Hermite transition between idle and attack", "idle", null),
    MESH("mesh", "Cubes plus poly_mesh geometry", "idle", null),
    TREE("tree", "Tree model mode comparison", "idle", "kltyton_bedrock_runtime/example_tree"),
    BAKED("baked", "Baked model mode comparison", "idle", "kltyton_bedrock_runtime/example_baked");

    public static final String MOD_ID = "kltyton_bedrock_runtime_examples";
    private static final Map<ExampleScene, GraphDefinition> GRAPHS = buildGraphs();

    private final String id;
    private final String title;
    private final String baseAnimation;
    private final String modelVariant;

    ExampleScene(String id, String title, String baseAnimation, String modelVariant) {
        this.id = id;
        this.title = title;
        this.baseAnimation = baseAnimation;
        this.modelVariant = modelVariant;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String baseAnimation() {
        return baseAnimation;
    }

    public String modelVariant() {
        return modelVariant;
    }

    public GraphDefinition graph() {
        return GRAPHS.get(this);
    }

    private static Map<ExampleScene, GraphDefinition> buildGraphs() {
        EnumMap<ExampleScene, GraphDefinition> graphs = new EnumMap<>(ExampleScene.class);
        graphs.put(SPEED_1D, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "parameters": {"speed": 0.0},
                  "output": "root",
                  "nodes": {
                    "idle": {"type": "animation", "animation": "idle", "time": "loop"},
                    "walk": {"type": "animation", "animation": "walk", "time": "loop"},
                    "run": {"type": "animation", "animation": "run", "time": "loop"},
                    "root": {
                      "type": "blend1d",
                      "parameter": "speed",
                      "entries": [
                        {"position": 0.0, "node": "idle"},
                        {"position": 1.0, "node": "walk"},
                        {"position": 2.0, "node": "run"}
                      ]
                    }
                  }
                }
                """));
        graphs.put(AIM_2D, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "parameters": {"yaw": 0.0, "pitch": 0.0},
                  "output": "root",
                  "nodes": {
                    "south_west": {"type": "animation", "animation": "aim_sw", "time": "loop"},
                    "south_east": {"type": "animation", "animation": "aim_se", "time": "loop"},
                    "north_west": {"type": "animation", "animation": "aim_nw", "time": "loop"},
                    "north_east": {"type": "animation", "animation": "aim_ne", "time": "loop"},
                    "root": {
                      "type": "blend2d",
                      "parameterX": "yaw",
                      "parameterY": "pitch",
                      "entries": [
                        {"x": -1.0, "y": -1.0, "node": "south_west"},
                        {"x": 1.0, "y": -1.0, "node": "south_east"},
                        {"x": -1.0, "y": 1.0, "node": "north_west"},
                        {"x": 1.0, "y": 1.0, "node": "north_east"}
                      ]
                    }
                  }
                }
                """));
        graphs.put(LEGS_AIM, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "parameters": {"aim": 1.0},
                  "output": "root",
                  "nodes": {
                    "locomotion": {"type": "animation", "animation": "walk", "time": "loop"},
                    "aim_pose": {"type": "animation", "animation": "aim_center", "time": "loop"},
                    "upper_aim": {
                      "type": "mask",
                      "node": "aim_pose",
                      "weight": 1.0,
                      "names": ["head", "body", "left_arm", "right_arm"]
                    },
                    "root": {
                      "type": "blend",
                      "base": "locomotion",
                      "input": "upper_aim",
                      "weightParameter": "aim"
                    }
                  }
                }
                """));
        graphs.put(ADDITIVE, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "parameters": {"gesture": 0.6},
                  "output": "root",
                  "nodes": {
                    "base": {"type": "animation", "animation": "idle", "time": "loop"},
                    "wave": {"type": "animation", "animation": "wave", "time": "loop"},
                    "root": {
                      "type": "additive",
                      "base": "base",
                      "additive": "wave",
                      "weightParameter": "gesture"
                    }
                  }
                }
                """));
        graphs.put(HERMITE, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "parameters": {"phase": 0.0},
                  "output": "root",
                  "nodes": {
                    "idle": {"type": "animation", "animation": "idle", "time": "loop"},
                    "attack": {"type": "animation", "animation": "attack", "time": "absolute", "loop": false},
                    "root": {
                      "type": "hermite",
                      "from": "idle",
                      "to": "attack",
                      "duration": 0.6,
                      "phaseParameter": "phase"
                    }
                  }
                }
                """));
        graphs.put(MESH, GraphJsonParser.parse("""
                {
                  "version": 1,
                  "output": "root",
                  "nodes": {
                    "root": {"type": "animation", "animation": "mesh_spin", "time": "loop"}
                  }
                }
                """));
        GraphDefinition idle = GraphJsonParser.parse("""
                {
                  "version": 1,
                  "output": "root",
                  "nodes": {
                    "root": {"type": "animation", "animation": "idle", "time": "loop"}
                  }
                }
                """);
        graphs.put(TREE, idle);
        graphs.put(BAKED, idle);
        return Map.copyOf(graphs);
    }
}
