package com.kltyton.bedrockruntime.mixin.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.storage.LevelStorageSource;
import com.kltyton.bedrockruntime.client.particle.bedrock.BedrockPackFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

@Mixin(Minecraft.class)
public abstract class MinecraftPacksMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"), index = 0)
    private RepositorySource[] kltyton_bedrock_runtime$addBedrockPacks(RepositorySource[] sources) {
        Minecraft client = (Minecraft) (Object) this;
        RepositorySource[] combined = Arrays.copyOf(sources, sources.length + 1);
        combined[sources.length] = new BedrockPackFinder(client.getResourcePackDirectory(), LevelStorageSource.parseValidator(client.gameDirectory.toPath().resolve("allowed_symlinks.txt")));
        return combined;
    }
}
