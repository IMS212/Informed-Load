package io.github.giantnuker.fabric.informedload.mixin;

import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.stream.Stream;

@Mixin(LanguageManager.class)
public interface LanguageManagerAccessor {
    @Invoker("method_29393")
    Map<String, LanguageDefinition> runReloadResources(Stream<ResourcePack> packs);
}
