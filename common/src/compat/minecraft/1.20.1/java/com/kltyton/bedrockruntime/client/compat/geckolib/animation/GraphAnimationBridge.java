package com.kltyton.bedrockruntime.client.compat.geckolib.animation;

import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;
import com.kltyton.bedrockruntime.animation.pose.PoseBuffer;
import com.kltyton.bedrockruntime.animation.pose.SkeletonBinding;
import com.kltyton.bedrockruntime.api.animation.GraphAnimatable;
import com.kltyton.bedrockruntime.client.animation.GraphWorldBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.model.GeoModel;

/** Immediate GeckoLib4 graph application boundary. Native controller processing is never advanced here. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class GraphAnimationBridge {
    private static final DataTicket<InstanceState> INSTANCE =
            new DataTicket<>("kltyton_bedrock_runtime_graph_instance", InstanceState.class);

    private GraphAnimationBridge() {
    }

    public static void apply(GeoModel<?> geckoModel, GeoAnimatable animatable, long instanceId,
            AnimationState<?> animationState, float partialTick) {
        GeoModel model = (GeoModel) geckoModel;
        ResourceLocation resource = model.getModelResource(animatable, null);
        BakedGeoModel bakedModel = GeckoLibCache.getBakedModels().get(resource);

        apply(geckoModel, animatable, instanceId, bakedModel, animationState, partialTick);
    }

    public static void apply(GeoModel<?> geckoModel, GeoAnimatable animatable, long instanceId,
            BakedGeoModel bakedModel, AnimationState<?> animationState, float partialTick) {
        if (!(animatable instanceof GraphAnimatable source)) {
            return;
        }

        GraphDefinition definition = source.animationGraph();

        if (definition == null) {
            return;
        }

        GeoModel model = (GeoModel) geckoModel;
        ResourceLocation resource = model.getModelResource(animatable, null);

        if (bakedModel == null && resource != null) {
            bakedModel = GeckoLibCache.getBakedModels().get(resource);
        }

        if (bakedModel == null) {
            throw new IllegalStateException("Missing baked GeckoLib model " + resource);
        }

        String resourceName = resource == null ? bakedModel.toString() : resource.toString();
        AnimatableManager manager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);
        InstanceState state = (InstanceState) manager.getData(INSTANCE);
        Object currentWorld = Minecraft.getInstance().level;

        if (state == null || state.model != bakedModel || !state.resource.equals(resourceName)
                || state.definition != definition || !state.world.matches(currentWorld)) {
            SkeletonBinding binding = GeckoSkeletonBindings.get(bakedModel, resourceName);
            GeoBone[] bones = new GeoBone[binding.size()];

            for (int i = 0; i < bones.length; i++) {
                bones[i] = bakedModel.getBone(binding.bone(i).name()).orElse(null);
            }

            state = new InstanceState(bakedModel, resourceName, definition, binding, new GraphRuntime(definition),
                    new Gecko4AnimationSampler(model, animatable), bones, new GraphWorldBinding(currentWorld));
            manager.setData(INSTANCE, state);
        }

        source.captureGraphParameters(state.runtime, relatedObject(animationState), partialTick);

        double seconds = animationState.getAnimationTick() / 20.0;

        if (seconds == 0.0 && state.lastSeconds > 0.0) {
            seconds = state.lastSeconds;
        }
        else {
            state.lastSeconds = seconds;
        }

        PoseBuffer pose = state.runtime.evaluate(instanceId, seconds, state.binding, state.sampler);

        apply(pose, state.bones);
    }

    private static Object relatedObject(AnimationState<?> state) {
        Object related = state.getData(DataTickets.ITEMSTACK);

        if (related == null) {
            related = state.getData(DataTickets.ENTITY);
        }

        if (related == null) {
            related = state.getData(DataTickets.BLOCK_ENTITY);
        }

        return related;
    }

    private static void apply(PoseBuffer pose, GeoBone[] bones) {
        for (int i = 0; i < bones.length; i++) {
            GeoBone bone = bones[i];

            if (bone == null) {
                continue;
            }

            if (pose.hasTranslation(i)) {
                bone.setPosX(pose.translationX(i));
                bone.setPosY(pose.translationY(i));
                bone.setPosZ(pose.translationZ(i));
            }

            if (pose.hasRotation(i)) {
                bone.setRotX(pose.rotationX(i));
                bone.setRotY(pose.rotationY(i));
                bone.setRotZ(pose.rotationZ(i));
            }

            if (pose.hasScale(i)) {
                bone.setScaleX(pose.scaleX(i));
                bone.setScaleY(pose.scaleY(i));
                bone.setScaleZ(pose.scaleZ(i));
            }
        }
    }

    private static final class InstanceState {
        private final BakedGeoModel model;
        private final String resource;
        private final GraphDefinition definition;
        private final SkeletonBinding binding;
        private final GraphRuntime runtime;
        private final Gecko4AnimationSampler sampler;
        private final GeoBone[] bones;
        private final GraphWorldBinding world;
        private double lastSeconds;

        private InstanceState(BakedGeoModel model, String resource, GraphDefinition definition,
                SkeletonBinding binding, GraphRuntime runtime, Gecko4AnimationSampler sampler, GeoBone[] bones,
                GraphWorldBinding world) {
            this.model = model;
            this.resource = resource;
            this.definition = definition;
            this.binding = binding;
            this.runtime = runtime;
            this.sampler = sampler;
            this.bones = bones;
            this.world = world;
        }
    }
}
