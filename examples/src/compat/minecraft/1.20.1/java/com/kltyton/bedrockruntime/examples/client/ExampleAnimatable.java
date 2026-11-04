package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;
import com.kltyton.bedrockruntime.api.animation.GraphAnimatable;
import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.examples.ExampleParameters;
import com.kltyton.bedrockruntime.examples.ExampleResources;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ExampleAnimatable implements GeoReplacedEntity, GraphAnimatable {
    private final ExampleScene scene;
    private final EntityType<?> replacingEntityType;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);
    private final RawAnimation baseAnimation;

    public ExampleAnimatable(ExampleScene scene, EntityType<?> replacingEntityType) {
        this.scene = scene;
        this.replacingEntityType = replacingEntityType;
        this.baseAnimation = RawAnimation.begin().thenLoop(scene.baseAnimation());
    }

    @Override
    public EntityType<?> getReplacingEntityType() {
        return replacingEntityType;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ExampleAnimatable>(this, "main", 0,
                state -> state.setAndContinue(baseAnimation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public GraphDefinition animationGraph() {
        if (scene == ExampleScene.MESH) {
            return EnhancedModels.graph(ExampleResources.humanoid());
        }
        return scene.graph();
    }

    @Override
    public void captureGraphParameters(GraphRuntime runtime, Object relatedObject, float partialTick) {
        ExampleParameters.capture(scene, runtime, relatedObject, Minecraft.getInstance().player, partialTick);
    }
}
