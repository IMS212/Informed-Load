package io.github.giantnuker.fabric.informedload.mixin;

import io.github.giantnuker.fabric.informedload.InformedLoadUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow public abstract ServerWorld getOverworld();

    @Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    private ServerWorld logTheWorld(MinecraftServer minecraftServer) {
        ServerWorld serverWorld = getOverworld();
        if (serverWorld.getClass() == ServerWorld.class) InformedLoadUtils.loadingWorld = serverWorld;
        return serverWorld;
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void clearILWorld(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        InformedLoadUtils.loadingWorld = null;
    }
}
