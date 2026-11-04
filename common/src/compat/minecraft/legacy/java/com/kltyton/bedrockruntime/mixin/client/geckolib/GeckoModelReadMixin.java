package com.kltyton.bedrockruntime.mixin.client.geckolib;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.EnhancedGeometryLoader;
import com.kltyton.bedrockruntime.client.compat.geckolib.loading.NativeModelMeshData;
import com.kltyton.bedrockruntime.model.resource.EnhancedModelData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.json.raw.Model;

/**
 * Uses a resource-local Gson clone with a corrected PolyMesh adapter. Other native adapters and
 * operation wrappers are preserved; the original Gson operation is always invoked exactly once.
 */
@Mixin(value = FileLoader.class, remap = false)
public final class GeckoModelReadMixin {
    @WrapOperation(method = "loadModelFile", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/Gson;fromJson(Lcom/google/gson/JsonElement;Ljava/lang/Class;)Ljava/lang/Object;"))
    private static Object kltyton_bedrock_runtime$readMesh(Gson gson, JsonElement json, Class<?> type, Operation<Object> original,
                                                @Local(argsOnly = true) ResourceLocation resource) {
        if (resource == null || !EnhancedGeometryLoader.shouldUse(resource)) {
            return original.call(gson, json, type);
        }
        EnhancedModelData data = EnhancedModelData.parse(resource.toString(), json.getAsJsonObject());
        Model model = (Model) original.call(NativeModelMeshData.localGson(gson), json, type);
        NativeModelMeshData.remember(model, data);
        return model;
    }
}
