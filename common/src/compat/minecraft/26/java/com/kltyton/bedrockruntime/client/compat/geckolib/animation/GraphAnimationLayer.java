package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.loading.math.MolangQueries;
import com.geckolib.loading.math.value.Variable;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.ClientUtil;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.api.animation.GraphAnimatable;
import com.kltyton.bedrockruntime.client.animation.GraphWorldBinding;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Captures game inputs and publishes an owned graph pose before native controller composition. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class GraphAnimationLayer extends GeoRenderLayer {
    private static final DataTicket<Instance> INSTANCE = DataTicket.create("kltyton_bedrock_runtime_graph_instance", Instance.class);
    private static final DataTicket<Input> INPUT = DataTicket.create("kltyton_bedrock_runtime_graph_input", Input.class);

    public GraphAnimationLayer(GeoRenderer renderer) { super(renderer); }

    @Override
    public void addRenderData(GeoAnimatable animatable, Object related, GeoRenderState state, float partialTick) {
        if (!(animatable instanceof GraphAnimatable source)) {
            state.addGeckolibData(INPUT, null);
            return;
        }
        GraphDefinition definition = source.animationGraph();
        AnimatableManager manager = state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        var resource = getGeoModel().getModelResource(state);
        BakedGeoModel model = getGeoModel().getBakedModel(resource);
        Instance instance = (Instance) manager.getAnimatableData(INSTANCE);
        if (instance == null || instance.model != model || instance.runtime.definition() != definition
                || !instance.world.matches(ClientUtil.getLevel())) {
            instance = new Instance(model, definition, GeckoSkeletonBindings.get(model, resource.toString()), ClientUtil.getLevel());
            manager.setAnimatableData(INSTANCE, instance);
        }
        source.captureGraphParameters(instance.runtime, related, partialTick);
        Map<String, Animation> animations = new HashMap<>();
        Set<Variable> variables = new HashSet<>();
        for (GraphDefinition.Node node : definition.nodes()) {
            if (node instanceof GraphDefinition.AnimationNode animationNode) {
                String name = animationNode.animation();
                Animation animation = getGeoModel().getBakedAnimation(animatable, name);
                if (animation == null) {
                    throw new IllegalArgumentException(resource + ": graph references missing animation '" + name + "'");
                }
                animations.put(name, animation);
                variables.addAll(animation.usedVariables());
            }
        }
        Reference2DoubleMap<Variable> queries = new Reference2DoubleOpenHashMap<>();
        if (ClientUtil.getLevel() != null && ClientUtil.getClientPlayer() != null) {
            MolangQueries.Actor<GeoAnimatable> actor = new MolangQueries.Actor<>(animatable, state, instance.queryController,
                    manager.getFirstRenderTick() - state.getAnimatableAge(), partialTick, ClientUtil.getLevel(),
                    ClientUtil.getClientPlayer(), ClientUtil.getCameraPos());
            MolangQueries.buildActorVariables(actor, variables, queries);
        }
        state.addGeckolibData(INPUT, new Input(instance, Map.copyOf(animations), queries,
                state.getAnimatableAge() / 20.0, Double.doubleToLongBits(state.getAnimatableAge())));
    }

    @Override
    public void preRender(RenderPassInfo pass, SubmitNodeCollector renderTasks) {
        Input input = (Input) pass.getGeckolibData(INPUT);
        if (input == null) {
            return;
        }
        input.pose(pass.renderState());
    }

    /** Inserts the graph as one controller, before native additive controllers or at the end if none exist. */
    public static void applyGraphPose(RenderPassInfo<?> pass, BoneSnapshots snapshots) {
        Input input = pass.getGeckolibData(INPUT);
        if (input == null) {
            return;
        }
        PoseBuffer pose = input.pose(pass.renderState());
        for (int bone = 0; bone < pose.size(); bone++) {
            if (!pose.hasTranslation(bone) && !pose.hasRotation(bone) && !pose.hasScale(bone)) {
                continue;
            }
            var binding = pose.binding().bone(bone);
            var target = snapshots.get(binding.name()).orElseThrow();
            if (pose.hasTranslation(bone)) {
                target.setTranslation(pose.translationX(bone), pose.translationY(bone), pose.translationZ(bone));
            }
            if (pose.hasRotation(bone)) {
                target.setRotation(pose.rotationX(bone) - binding.bindRotX(),
                        pose.rotationY(bone) - binding.bindRotY(), pose.rotationZ(bone) - binding.bindRotZ());
            }
            if (pose.hasScale(bone)) {
                target.setScale(pose.scaleX(bone), pose.scaleY(bone), pose.scaleZ(bone));
            }
        }
    }

    private static final class Input {
        private final Instance instance;
        private final Map<String, Animation> animations;
        private final Reference2DoubleMap<Variable> queries;
        private final double seconds;
        private final long frame;
        private PoseBuffer pose;

        private Input(Instance instance, Map<String, Animation> animations, Reference2DoubleMap<Variable> queries,
                      double seconds, long frame) {
            this.instance = instance;
            this.animations = animations;
            this.queries = queries;
            this.seconds = seconds;
            this.frame = frame;
        }

        private PoseBuffer pose(GeoRenderState state) {
            if (pose == null) {
                instance.sampler.capture(animations, state, queries);
                try {
                    PoseBuffer result = instance.runtime.evaluate(frame, seconds, instance.binding, instance.sampler);
                    PoseBuffer captured = new PoseBuffer(instance.binding);
                    captured.copyFrom(result);
                    pose = captured;
                } finally {
                    instance.sampler.releaseCapture();
                }
            }
            return pose;
        }
    }

    private static final class Instance {
        final BakedGeoModel model;
        final SkeletonBinding binding;
        final GraphRuntime runtime;
        final GeckoAnimationSampler sampler;
        final GraphWorldBinding world;
        final AnimationController<GeoAnimatable> queryController = new AnimationController<>("kltyton_bedrock_runtime_graph", 0,
                test -> PlayState.CONTINUE);

        Instance(BakedGeoModel model, GraphDefinition graph, SkeletonBinding binding, Object world) {
            this.model = model;
            this.binding = binding;
            this.runtime = new GraphRuntime(graph);
            this.sampler = new GeckoAnimationSampler(model);
            this.world = new GraphWorldBinding(world);
        }
    }
}
