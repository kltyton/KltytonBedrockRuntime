package com.kltyton.bedrockruntime.examples.client;

import com.kltyton.bedrockruntime.examples.ExampleScene;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public final class ExampleModel extends DefaultedEntityGeoModel<ExampleAnimatable> {
    public ExampleModel(ExampleScene scene) {
        super(ExampleIds.id("kltyton_bedrock_runtime/example_humanoid"));
        if (scene.modelVariant() != null) {
            withAltModel(ExampleIds.id(scene.modelVariant()));
        }
    }
}
