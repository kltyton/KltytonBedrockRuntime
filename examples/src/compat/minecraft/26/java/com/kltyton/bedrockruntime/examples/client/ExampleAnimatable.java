package com.kltyton.bedrockruntime.examples.client;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.kltyton.bedrockruntime.animation.graph.GraphDefinition;
import com.kltyton.bedrockruntime.animation.graph.GraphRuntime;
import com.kltyton.bedrockruntime.api.animation.GraphAnimatable;
import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.examples.ExampleParameters;
import com.kltyton.bedrockruntime.examples.ExampleResources;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.client.Minecraft;

public final class ExampleAnimatable implements GeoReplacedEntity, GraphAnimatable {
    private final ExampleScene scene;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);
    private final RawAnimation baseAnimation;
    private final RawAnimation additiveAnimation;

    public ExampleAnimatable(ExampleScene scene) {
        this.scene = scene;
        this.baseAnimation = RawAnimation.begin().thenLoop(scene.baseAnimation());
        this.additiveAnimation = RawAnimation.begin().thenLoop("breath");
    }

    public ExampleScene scene() {
        return scene;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ExampleAnimatable>("main", 0,
                state -> state.setAndContinue(baseAnimation)));
        if (scene == ExampleScene.ADDITIVE) {
            controllers.add(new AnimationController<ExampleAnimatable>("native_additive", 0,
                    state -> state.setAndContinue(additiveAnimation)).additiveAnimations());
        }
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
