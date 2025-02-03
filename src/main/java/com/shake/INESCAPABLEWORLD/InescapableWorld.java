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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;

@Mod("inescapableworld")
public class InescapableWorld {
    public static final String MOD_ID = "inescapableworld";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/world_anchor.png");
    private static boolean showIcon = true;
    private static final KeyMapping TOGGLE_ICON_KEY = new KeyMapping(
            "key.inescapableworld.toggle_icon",
            GLFW.GLFW_KEY_H,
            "key.categories.misc"
    );

    public InescapableWorld() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderGUI);
        MinecraftForge.EVENT_BUS.addListener(this::onKeyInput);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("INESCAPABLE WORLD Mod is loading...");
        preventWindowClosing();
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_ICON_KEY);
    }
    private void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        blockExitButtons(screen);
    }

    private void onTick(TickEvent.ClientTickEvent event) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof PauseScreen || currentScreen instanceof DisconnectedScreen) {
            blockExitButtons(currentScreen);
        }
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

    private void preventWindowClosing() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetWindowCloseCallback(windowHandle, (window) -> {
            LOGGER.warn("Attempt to close the game blocked!");
            GLFW.glfwSetWindowShouldClose(window, false);
        });
        GLFW.glfwSetWindowShouldClose(windowHandle, false);
    }
}
