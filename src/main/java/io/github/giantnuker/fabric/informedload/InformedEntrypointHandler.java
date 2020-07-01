package io.github.giantnuker.fabric.informedload;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.giantnuker.fabric.informedload.api.ProgressBar;
import io.github.giantnuker.fabric.informedload.mixin.MinecraftClientAccessor;
import io.github.giantnuker.fabric.loadcatcher.EntrypointCatcher;
import io.github.giantnuker.fabric.loadcatcher.EntrypointHandler;
import io.github.giantnuker.fabric.loadcatcher.EntrypointKind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.RunArgs;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.WindowProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;
import static net.minecraft.client.gui.DrawableHelper.drawTexture;
import static net.minecraft.client.gui.DrawableHelper.fill;

public class InformedEntrypointHandler implements EntrypointHandler {
    public static boolean STARTED = false;
    private static volatile InformedEntrypointHandler INSTANCE;
    public static RunArgs args;
    public static boolean isPreloading = true;

    public static void runThreadingBypassHandler(File newRunDir, Object gameInstance) {
        try {
            // Window creation and init
            MinecraftClientAccessor mc = ((MinecraftClientAccessor) MinecraftClient.getInstance());
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            //mc.runStartTimerHackThread();
            MinecraftClientAccessor.getLOGGER().info("Backend library: {}", RenderSystem.getBackendDescription());
            WindowSettings windowSettings2;
            GameOptions options = new GameOptions(MinecraftClient.getInstance(), MinecraftClient.getInstance().runDirectory);

            Field optionsField = MinecraftClient.class.getDeclaredField(FabricLoader.INSTANCE.getMappingResolver().mapFieldName("intermediary", "net.minecraft.class_310", "field_1690", "Lnet/minecraft/class_315;"));
            //                                                                                                                                        net.minecraft.client.MinecraftClient options net.minecraft.client.options.GameOptions
            modifiersField.setInt(optionsField, optionsField.getModifiers() & ~Modifier.FINAL);
            optionsField.setAccessible(true);
            optionsField.set(MinecraftClient.getInstance(), options);

            //MinecraftClient.getInstance().options = options;
            //if (options.overrideHeight > 0 && options.overrideWidth > 0) {
            windowSettings2 = new WindowSettings(/*options.overrideWidth, options.overrideHeight*/750, 500, OptionalInt.empty(), OptionalInt.empty(), false);
            //} else {
            //    windowSettings2 = args.windowSettings;
            //}

            Util.nanoTimeSupplier = RenderSystem.initBackendSystem();
            Field windowProviderField = MinecraftClient.class.getDeclaredField(FabricLoader.INSTANCE.getMappingResolver().mapFieldName("intermediary", "net.minecraft.class_310", "field_1686", "Lnet/minecraft/class_3682;"));
            //                                                                                                                                        net.minecraft.client.MinecraftClient windowProvider net.minecraft.client.util.WindowProvider
            modifiersField.setInt(windowProviderField, windowProviderField.getModifiers() & ~Modifier.FINAL);
            windowProviderField.setAccessible(true);
            windowProviderField.set(MinecraftClient.getInstance(), new WindowProvider(MinecraftClient.getInstance()));
            mc.setWindow(mc.getWindowProvider().createWindow(windowSettings2, options.fullscreenResolution, mc.runGetWindowTitle()));
            mc.setMouse(new Mouse((MinecraftClient) mc));
            mc.runOnWindowFocusChanged(true);

            try {
                InputStream inputStream = mc.runGetResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_16x16.png"));
                InputStream inputStream2 = mc.runGetResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_32x32.png"));
                MinecraftClient.getInstance().getWindow().setIcon(inputStream, inputStream2);
            } catch (IOException var8) {
                MinecraftClientAccessor.getLOGGER().error("Couldn't set icon", var8);
            }

            MinecraftClient.getInstance().getWindow().setFramerateLimit(options.maxFps);
            //this.mouse = new Mouse(this);
            //this.mouse.setup(MinecraftClient.getInstance().getWindow().getHandle());
            //this.keyboard = new Keyboard(this);
            //this.keyboard.setup(MinecraftClient.getInstance().getWindow().getHandle());
            RenderSystem.initRenderer(options.glDebugVerbosity, false);
            Field framebufferField = MinecraftClient.class.getDeclaredField(FabricLoader.INSTANCE.getMappingResolver().mapFieldName("intermediary", "net.minecraft.class_310", "field_1689", "Lnet/minecraft/class_276;"));
            //                                                                                                                                      net.minecraft.client.MinecraftClient framebuffer net.minecraft.client.gl.Framebuffer
            modifiersField.setInt(framebufferField, framebufferField.getModifiers() & ~Modifier.FINAL);
            framebufferField.setAccessible(true);
            framebufferField.set(MinecraftClient.getInstance(), new Framebuffer(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight(), true, IS_SYSTEM_MAC));
            mc.getFramebuffer().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

            InformedLoadUtils.isDoingEarlyLoad = true;
            RenderSystem.setupDefaultState(0, 0, MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight());


            // Setup Informed Load hooks
            ReloadableResourceManagerImpl resourceManager = new ReloadableResourceManagerImpl(ResourceType.CLIENT_RESOURCES);
            options.addResourcePackProfilesToManager(mc.getResourcePackManager());
            mc.getResourcePackManager().scanPacks();

            List<ResourcePack> list = mc.getResourcePackManager().getEnabledProfiles().stream().map(ResourcePackProfile::createResourcePack).collect(Collectors.toList());
            for (ResourcePack resourcePack_1 : list) {
                resourceManager.addPack(resourcePack_1);
            }

            LanguageManager languageManager = new LanguageManager(options.language);
            resourceManager.registerListener(languageManager);
            //((LanguageManagerAccessor) languageManager).runReloadResources(list.stream());
            InformedLoadUtils.textureManager = new TextureManager(resourceManager);

            int i = MinecraftClient.getInstance().getWindow().calculateScaleFactor(options.guiScale, options.forceUnicodeFont);
            MinecraftClient.getInstance().getWindow().setScaleFactor(i);

            Framebuffer framebuffer = mc.getFramebuffer();
            framebuffer.resize(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight(), IS_SYSTEM_MAC);

            FontManager fontManager = new FontManager(InformedLoadUtils.textureManager);
            resourceManager.registerListener(fontManager.getResourceReloadListener());

            //final FontStorage fontStorage_1 = new FontStorage(InformedLoadUtils.textureManager, new Identifier("loading"));
            //fontStorage_1.setFonts(Collections.singletonList(FontType.BITMAP.createLoader(new JsonParser().parse(InformedLoadUtils.FONT_JSON).getAsJsonObject()).load(resourceManager)));
            InformedLoadUtils.textRenderer = fontManager.createTextRenderer();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        STARTED = true;
        Thread loaderThread = new Thread(() -> EntrypointCatcher.runEntrypointRedirection(newRunDir, gameInstance)); // Doing this allows someone else to redirect it still
        loaderThread.start();
        while (INSTANCE == null) {
        }
        while (loaderThread.isAlive()) {
            INSTANCE.render();
        }
        isPreloading = false;
    }

    List<ProgressBar> progressBars = new ArrayList<>();
    String subText1 = "", subText2 = "";
    boolean keepRendering = true;
    public static boolean endPrerender = false;
    private static final Identifier LOGO = new Identifier("textures/gui/title/mojangstudios.png");

    private static final int field_25042 = BackgroundHelper.ColorMixer.getArgb(255, 239, 50, 61) & 16777215;

    private void render() {
        MatrixStack matrices = new MatrixStack();
        MinecraftClient client = MinecraftClient.getInstance();
        int i = client.getWindow().getScaledWidth();
        int j = client.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        //if (this.reloading && (this.reloadMonitor.isPrepareStageComplete() || client.currentScreen != null) && this.prepareCompleteTime == -1L) {
        //    this.prepareCompleteTime = l;
        //}

        //float f = this.applyCompleteTime > -1L ? (float)(l - this.applyCompleteTime) / 1000.0F : -1.0F;
        //float g = this.prepareCompleteTime > -1L ? (float)(l - this.prepareCompleteTime) / 500.0F : -1.0F;
        float o;
        int m;
        //if (client.currentScreen != null) {
        //    client.currentScreen.render(matrices, 0, 0, delta);
        //}
        m = 255;
        Window window = MinecraftClient.getInstance().getWindow();
        RenderSystem.pushMatrix();
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, (double)window.getFramebufferWidth() / window.getScaleFactor(), (double)window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);

        fill(matrices, 0, 0, i, j, field_25042 | 255 << 24);

        m = (int) ((double) client.getWindow().getScaledWidth() * 0.5D);
        int q = (int) ((double) client.getWindow().getScaledHeight() * 0.5D);
        double d = Math.min((double) client.getWindow().getScaledWidth() * 0.75D, (double) client.getWindow().getScaledHeight()) * 0.25D;
        int r = (int) (d * 0.5D);
        r += 40;
        double e = d * 4.0D;
        int s = (int) (e * 0.5D);
        client.getTextureManager().bindTexture(LOGO);
        RenderSystem.enableBlend();
        RenderSystem.blendEquation(32774);
        RenderSystem.blendFunc(770, 1);
        RenderSystem.alphaFunc(516, 0.0F);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0f);
        drawTexture(matrices, m - s, q - r, s, (int) d, -0.0625F, 0.0F, 120, 60, 120, 120);
        drawTexture(matrices, m, q - r, s, (int) d, 0.0625F, 60.0F, 120, 60, 120, 120);
        RenderSystem.defaultBlendFunc();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.disableBlend();
        //int t = (int) ((double) client.getWindow().getScaledHeight() * 0.8325D);
        //float u = this.reloadMonitor.getProgress();
        //this.progress = MathHelper.clamp(this.progress * 0.95F + u * 0.050000012F, 0.0F, 1.0F);
        //if (f < 1.0F) {
        //    this.renderProgressBar(matrices, i / 2 - s, t - 5, i / 2 + s, t + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
        //}

        //if (f >= 2.0F) {
            //client.setOverlay((Overlay) null);
        //}

        /*if (this.applyCompleteTime == -1L && this.reloadMonitor.isApplyStageComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reloadMonitor.throwExceptions();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                this.exceptionHandler.accept(Optional.of(var23));
            }

            this.applyCompleteTime = Util.getMeasuringTimeMs();
            if (client.currentScreen != null) {
                client.currentScreen.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            }
        }*/
        RenderSystem.popMatrix();
        window.swapBuffers();
        //glfwSwapBuffers(MinecraftClient.getInstance().getWindow().getHandle());
        if (GLX._shouldClose(MinecraftClient.getInstance().getWindow())) {
            MinecraftClient.getInstance().stop();
        }
    }

    private void renderSubText(String text, int row) {
//        InformedLoadUtils.textRenderer.draw(text, MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f - InformedLoadUtils.textRenderer.getWidth(text) / 2f, MinecraftClient.getInstance().getWindow().getScaledHeight() - (row + 1) * 20, 0x666666);
    }

    public ProgressBar createProgressBar(int row, ProgressBar.SplitType splitType) {
        return new ProgressBar.SplitProgressBar(splitType) {
            @Override
            protected int getY(Window window) {
                return row * 20 + window.getScaledHeight() / 4 * 3 - 40;
            }
        };
    }

    Map<String, ModContainer> mainToContainer;
    Map<String, ModContainer> clientToContainer;

    AtomicInteger index;
    AtomicInteger total;

    ProgressBar overall;
    ProgressBar commonEntrypoints;
    ProgressBar clientEntrypoints;

    int totalClientEntrypoints;

    @Override
    public void beforeModsLoaded() {
        //runThreadingBypassHandler(MinecraftClient.getInstance().runDirectory, MinecraftClient.getInstance());
        INSTANCE = this;
        progressBars.clear();
        overall = createProgressBar(0, ProgressBar.SplitType.NONE);
        progressBars.add(overall);
        overall.setText("Locating Entrypoints");
        InformedLoadUtils.LOGGER.info("Locating Entrypoints");
        mainToContainer = new HashMap<>();
        clientToContainer = new HashMap<>();

        InformedLoadUtils.LOGGER.info("Loading Mods");
        int totalMainEntrypoints = FabricLoader.INSTANCE.getEntrypoints("main", ModInitializer.class).size();
        totalClientEntrypoints = FabricLoader.INSTANCE.getEntrypoints("client", ClientModInitializer.class).size();
        commonEntrypoints = createProgressBar(1, ProgressBar.SplitType.LEFT);
        commonEntrypoints.setText(totalMainEntrypoints + " Common");
        clientEntrypoints = createProgressBar(1, ProgressBar.SplitType.RIGHT);
        clientEntrypoints.setText(totalClientEntrypoints + " Client");

        index = new AtomicInteger();
        total = new AtomicInteger(totalMainEntrypoints);

        progressBars.add(commonEntrypoints);
        progressBars.add(clientEntrypoints);
    }

    @Override
    public void beforeModInitEntrypoint(String id, ModContainer mod, EntrypointKind entrypointKind) {
        overall.setText("Running Entrypoints - " + (entrypointKind == EntrypointKind.CLIENT ? "Client" : "Common"));
        if (entrypointKind == EntrypointKind.CLIENT && totalClientEntrypoints != -1) {
            commonEntrypoints.setText("Common Complete");
            total.set(totalClientEntrypoints);
            index.set(0);
            totalClientEntrypoints = -1;
        }
        index.set(index.get() + 1);
        subText1 = "";
        subText2 = "";
        ModMetadata metadata = mod != null ? mod.getMetadata() : null;
        if (metadata != null) {
            subText1 = metadata.getName() + " (" + metadata.getId() + ")";
        } else {
            subText1 = "UNKNOWN MOD";
        }
        subText2 = id;

        InformedLoadUtils.logDebug(metadata == null ? String.format("Loading [UNKNOWN MOD]: %s (%s)", id, entrypointKind == EntrypointKind.CLIENT ? "Client" : "Main") : String.format("Loading %s(%s): %s (%s)", metadata.getName(), metadata.getId(), id, entrypointKind == EntrypointKind.CLIENT ? "Client" : "Main"));
    }

    @Override
    public void afterModInitEntrypoint(String id, ModContainer mod, EntrypointKind entrypointKind) {
        switch (entrypointKind) {
            case CLIENT:
                clientEntrypoints.setText(index.get() + "/" + total.get() + " Client");
                clientEntrypoints.setProgress((float) (index.get()) / total.get());
                overall.setProgress((0.5f + (((float) (index.get()) / total.get()) / 2f)) / 2f);
                break;
            case COMMON:
                commonEntrypoints.setText(index.get() + "/" + total.get() + " Common");
                commonEntrypoints.setProgress((float) (index.get()) / total.get());
                overall.setProgress((((float) (index.get()) / total.get()) / 2f) / 2f);
                break;
        }
    }
}
