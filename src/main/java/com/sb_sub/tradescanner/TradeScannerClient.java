package com.sb_sub.tradescanner;

import com.sb_sub.tradescanner.mixin.KeyBindingAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class TradeScannerClient implements ClientModInitializer {
    private static KeyBinding scanKey;
    private static boolean wasScanKeyDown = false;
    private static Screen lastScreen = null;

    @Override
    public void onInitializeClient() {
        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tradescanner.scan",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.Category.create(Identifier.of("tradescanner", "silverbull_calc"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!(client.currentScreen instanceof HandledScreen<?>)) {
                TradeMarketScanner.clearHighlights();
                lastScreen = client.currentScreen;
                updateInputState(client);
                return;
            }

            if (client.currentScreen != lastScreen) {
                TradeMarketScanner.clearHighlights();
                lastScreen = client.currentScreen;
            }

            if (isScanKeyPressed(client)) {
                if (!wasScanKeyDown) {
                    if (TradeMarketScanner.hasHighlights()) {
                        TradeMarketScanner.clearHighlights();
                    } else {
                        TradeMarketScanner.scan(client);
                    }
                }

                wasScanKeyDown = true;
            } else {
                wasScanKeyDown = false;
            }
        });

        System.out.println("Silverbull Calc loaded");
    }

    private static void updateInputState(MinecraftClient client) {
        wasScanKeyDown = isScanKeyPressed(client);
    }

    private static boolean isScanKeyPressed(MinecraftClient client) {
        if (client.getWindow() == null || scanKey == null || scanKey.isUnbound()) {
            return false;
        }

        InputUtil.Key boundKey = ((KeyBindingAccessor) scanKey).tradescanner$getBoundKey();

        if (boundKey.getCategory() != InputUtil.Type.KEYSYM) {
            return false;
        }

        return InputUtil.isKeyPressed(client.getWindow(), boundKey.getCode());
    }
}