package com.kltyton.bedrockruntime.examples.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.kltyton.bedrockruntime.examples.ExampleScene;
import net.minecraft.resources.Identifier;

public final class ExampleModel extends DefaultedEntityGeoModel<ExampleAnimatable> {
    public ExampleModel(ExampleScene scene) {
        super(Identifier.fromNamespaceAndPath(ExampleScene.MOD_ID, "kltyton_bedrock_runtime/example_humanoid"));
        if (scene.modelVariant() != null) {
            withAltModel(Identifier.fromNamespaceAndPath(ExampleScene.MOD_ID, scene.modelVariant()));
        }
    }
}
