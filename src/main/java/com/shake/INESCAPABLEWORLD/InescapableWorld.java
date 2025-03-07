package com.shake.INESCAPABLEWORLD;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Mod("inescapableworld")
public class InescapableWorld {
    public static final String MOD_ID = "inescapableworld";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_PATH = Path.of("config/inescapableworld-config.json");

    private boolean disclaimerChecked = false; // 免責事項が表示されたかどうか


    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/world_anchor.png");
    private static boolean showIcon = true;
    private static final KeyMapping TOGGLE_ICON_KEY = new KeyMapping(
            "key.inescapableworld.toggle_icon",
            GLFW.GLFW_KEY_H,
            "key.categories.misc"
    );

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_ICON_KEY);
    }
    private void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        blockExitButtons(screen);
    }
    private void blockExitButtons(Screen screen) {
        if (screen == null) return;

        List<Button> buttons = screen.children().stream()
                .filter(widget -> widget instanceof Button)
                .map(widget -> (Button) widget)
                .toList();

        for (Button button : buttons) {
            Component message = button.getMessage();
            boolean shouldBlock =
                    (screen instanceof PauseScreen &&
                            (message.equals(Component.translatable("menu.disconnect")) ||
                                    message.equals(Component.translatable("menu.quit")) ||
                                    message.equals(Component.translatable("menu.returnToMenu")))) ||
                            (screen instanceof DisconnectedScreen &&
                                    message.equals(Component.translatable("menu.quit")));

            if (shouldBlock) {
                button.setMessage(Component.literal("§c[BLOCKED]")); // ボタンのラベルを変更
                button.active = false; // ボタンを無効化
                LOGGER.info("Blocked button: " + message.getString());
            }
        }
    }

    private void onRenderGUI(RenderGuiEvent.Post event) {
        if (!showIcon) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int iconSize = 32;

        RenderSystem.setShaderTexture(0, ICON_TEXTURE);
        guiGraphics.blit(ICON_TEXTURE, screenWidth - iconSize - 5, screenHeight - iconSize - 5, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }

    private void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && TOGGLE_ICON_KEY.isDown()) {
            showIcon = !showIcon;
            LOGGER.info("WorldAnchor icon visibility toggled: " + showIcon);
        }
    }
    public InescapableWorld() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderGUI);
        MinecraftForge.EVENT_BUS.addListener(this::onKeyInput);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("INESCAPABLE WORLD Mod is loading...");
    }

    private void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (disclaimerChecked || mc.screen instanceof DisclaimerScreen) return;
        if (mc.screen instanceof TitleScreen) {
            LOGGER.info("Opening Disclaimer Screen before Title Screen...");
            disclaimerChecked = true;
            mc.setScreen(new DisclaimerScreen());
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof PauseScreen || currentScreen instanceof DisconnectedScreen) {
                blockExitButtons(currentScreen);
            }
        }
    }

    private boolean hasUserAgreed() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                return json.has("agreed") && json.get("agreed").getAsBoolean();
            } catch (IOException e) {
                LOGGER.error("Failed to read config file: " + CONFIG_PATH, e);
            }
        }
        return false;
    }

    private void saveUserAgreement() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("agreed", true);
            Files.writeString(CONFIG_PATH, json.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: " + CONFIG_PATH, e);
        }
    }

    private class DisclaimerScreen extends Screen {
        private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("textures/gui/title/background/panorama_0.png");

        protected DisclaimerScreen() {
            super(Component.literal("Inescapable World - Disclaimer"));
        }
        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.addRenderableWidget(Button.builder(Component.literal("I Agree"), (button) -> {
                saveUserAgreement();
                Minecraft.getInstance().setScreen(new TitleScreen());
            }).pos(centerX - 75, centerY + 50).size(150, 20).build());
        }
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(guiGraphics);
            RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
            guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, 256, 256);
            guiGraphics.drawCenteredString(this.font, "INESCAPABLE WORLD Mod - Disclaimer", this.width / 2, this.height / 2 - 80, 0xFF5555);
            guiGraphics.drawCenteredString(this.font, "This mod restricts game exit options.", this.width / 2, this.height / 2 - 60, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "By using this mod, you agree to these restrictions.", this.width / 2, this.height / 2 - 40, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "If you do not agree, please exit the game.", this.width / 2, this.height / 2 - 20, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "To exit, press ALT+F4 or close the game from Task Manager.", this.width / 2, this.height / 2 + 10, 0xFFAAAA);

            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
}
