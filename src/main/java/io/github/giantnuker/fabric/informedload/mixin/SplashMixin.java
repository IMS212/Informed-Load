package io.github.giantnuker.fabric.informedload.mixin;

import com.google.gson.JsonParser;
import io.github.giantnuker.fabric.informedload.InformedEntrypointHandler;
import io.github.giantnuker.fabric.informedload.InformedLoadUtils;
import io.github.giantnuker.fabric.informedload.TaskList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.FontType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReloadMonitor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Indigo Amann
 */
@Mixin(SplashScreen.class)
public abstract class SplashMixin extends Overlay {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void changeBackColor(MinecraftClient client, ResourceReloadMonitor monitor, Consumer<Optional<Throwable>> exceptionHandler, boolean reloading, CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        Field colorField = SplashScreen.class.getDeclaredField("field_25042");
        InformedEntrypointHandler.modifiersField.setInt(colorField, colorField.getModifiers() & ~Modifier.FINAL);
        colorField.set(this, InformedLoadUtils.config.theme.ibackground & 16777215);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashScreen;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"))
    private void fillBkg(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(matrices, x1, y1, x2, y2, (InformedLoadUtils.config.theme.ibackground & 16777215) | (color >>> 24) << 24);
    }

    @Shadow
    private MinecraftClient client;
    @Shadow
    private float progress;
    @Shadow
    private void renderProgressBar(MatrixStack matrixStack, int int_1, int int_2, int int_3, int int_4, float float_1) {}
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashScreen;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIFFIIII)V"))
    public void translateLogo(MatrixStack matrices, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        DrawableHelper.drawTexture(matrices, x, y - 40, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashScreen;renderProgressBar(Lnet/minecraft/client/util/math/MatrixStack;IIIIF)V"))
    public void swapProgressRender(SplashScreen dis, MatrixStack matrixStack, int x, int y, int end_x, int end_y, float fade) {
        int window_width = this.client.getWindow().getScaledWidth();
        int window_height = this.client.getWindow().getScaledHeight();
        y = window_height / 4 * 3 - 40;
        InformedLoadUtils.renderProgressBar(matrixStack, window_width / 2 - 150, y, window_width / 2 + 150, y + 10, this.progress, fade);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    public void setup(CallbackInfo ci) {
        InformedLoadUtils.isDoingEarlyLoad = false;
        //MinecraftClient client = MinecraftClient.getInstance();
       // final FontStorage fontStorage_1 = new FontStorage(client.getTextureManager(), new Identifier("loading"));
        //fontStorage_1.setFonts(Collections.singletonList(FontType.BITMAP.createLoader(new JsonParser().parse(InformedLoadUtils.FONT_JSON).getAsJsonObject()).load(client.getResourceManager())));
        //InformedLoadUtils.textRenderer = new TextRenderer(client.getTextureManager(), fontStorage_1);
        final FontStorage fontStorage_1 = new FontStorage(client.getTextureManager(), new Identifier("loading"));
        fontStorage_1.setFonts(Collections.singletonList(FontType.BITMAP.createLoader(new JsonParser().parse(InformedLoadUtils.FONT_JSON).getAsJsonObject()).load(client.getResourceManager())));

        InformedLoadUtils.textRenderer = new TextRenderer(id -> fontStorage_1);
    }
}
