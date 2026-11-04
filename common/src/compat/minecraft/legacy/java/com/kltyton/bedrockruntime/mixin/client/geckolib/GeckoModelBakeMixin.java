package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.kltyton.bedrockruntime.api.model.EnhancedModels;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.NativeModelMeshData;
import com.kltyton.bedrockruntime.client.compat.geckolib.optimization.GL4CompiledGeometry;
import com.kltyton.bedrockruntime.model.binding.ModelConsumer;
import com.kltyton.bedrockruntime.model.binding.ModelConsumers;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

/**
 * Always invokes the original GeckoLib factory exactly once. For opted-in resources, metadata is
 * attached to the original bones and the original model is used for optional compiled geometry.
 */
@Mixin(value = GeckoLibCache.class, remap = false)
public class GeckoModelBakeMixin {
    @WrapOperation(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/loading/object/BakedModelFactory;constructGeoModel(Lsoftware/bernie/geckolib/loading/object/GeometryTree;)Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;"))
    private static BakedGeoModel kltyton_bedrock_runtime$scopeFactory(BakedModelFactory factory, GeometryTree geometryTree,
                                                            Operation<BakedGeoModel> original,
                                                            @Local(argsOnly = true) ResourceLocation resource) {
        BakedGeoModel model = original.call(factory, geometryTree);
        if (resource == null || !EnhancedGeometryLoader.shouldUse(resource)) {
            return model;
        }
        EnhancedModelData data = NativeModelMeshData.require(geometryTree.properties(), resource.toString());
        NativeModelMeshData.attach(model, geometryTree, resource.toString(), data,
                EnhancedGeometryLoader.doubleSided(resource));
        if (data.mode() == EnhancedModelData.Mode.BAKED) {
            GL4CompiledGeometry.compile(data, model, compileConsumers(resource, model));
        }
        return model;
    }

    private static ModelConsumers compileConsumers(ResourceLocation resource, BakedGeoModel model) {
        ModelConsumers declared = EnhancedModels.consumers(resource.toString());
        ModelConsumers.Builder builder = ModelConsumers.builder();
        for (ModelConsumer consumer : declared.activeConsumers()) {
            builder.addAll(consumer, declared.bones(consumer));
        }
        for (GeoBone root : model.topLevelBones()) {
            appendCustomBones(root, builder);
        }
        return builder.build();
    }

    private static void appendCustomBones(GeoBone bone, ModelConsumers.Builder builder) {
        if (bone.getClass() != GeoBone.class) {
            builder.add(ModelConsumer.PER_BONE_CALLBACK, bone.getName());
        }
        for (GeoBone child : bone.getChildBones()) {
            appendCustomBones(child, builder);
        }
    }
}
