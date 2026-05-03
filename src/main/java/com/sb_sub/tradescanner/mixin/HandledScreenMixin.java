package com.sb_sub.tradescanner.mixin;

import com.sb_sub.tradescanner.TradeMarketScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Inject(method = "render", at = @At("RETURN"))
    private void tradescanner$renderHighlights(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        for (Slot slot : handler.slots) {
            TradeMarketScanner.HighlightType type = TradeMarketScanner.getHighlightType(slot.id);

            if (type == null) {
                continue;
            }

            int slotX = x + slot.x;
            int slotY = y + slot.y;

            context.fill(slotX, slotY, slotX + 16, slotY + 16, getColor(type));
        }

        if (TradeMarketScanner.hasHighlights()) {
            drawLegend(context, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void tradescanner$clearOnSlotClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!TradeMarketScanner.hasHighlights()) {
            return;
        }

        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        for (Slot slot : handler.slots) {
            int slotX = x + slot.x;
            int slotY = y + slot.y;

            boolean overSlot = click.x() >= slotX
                    && click.x() < slotX + 16
                    && click.y() >= slotY
                    && click.y() < slotY + 16;

            if (overSlot) {
                TradeMarketScanner.clearHighlights();
                return;
            }
        }
    }

    private int getColor(TradeMarketScanner.HighlightType type) {
        return switch (type) {
            case HAS_SB -> 0x668FEBFF;
            case NO_SB -> 0x44FF0000;
            case LBO -> 0x449000FF;
        };
    }

    private void drawLegend(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.textRenderer == null) {
            return;
        }

        int legendX = x + 185;
        int legendY = y + 12;

        drawLegendSquare(context, legendX, legendY, 0x668FEBFF);
        drawLegendSquare(context, legendX, legendY + 14, 0x44FF0000);
        drawLegendSquare(context, legendX, legendY + 28, 0x449000FF);

        if (isMouseOver(mouseX, mouseY, legendX, legendY, 10, 10)) {
            context.drawTooltip(
                    client.textRenderer,
                    List.of(Text.literal("Has SB: price is under the active SB threshold")),
                    mouseX,
                    mouseY
            );
        } else if (isMouseOver(mouseX, mouseY, legendX, legendY + 14, 10, 10)) {
            context.drawTooltip(
                    client.textRenderer,
                    List.of(Text.literal("No SB: price is not under an SB threshold")),
                    mouseX,
                    mouseY
            );
        } else if (isMouseOver(mouseX, mouseY, legendX, legendY + 28, 10, 10)) {
            context.drawTooltip(
                    client.textRenderer,
                    List.of(Text.literal("LBO: Lowest Buy Offer found in this scan")),
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawLegendSquare(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 10, y + 10, color);
        context.fill(x, y, x + 10, y + 1, 0xFFFFFFFF);
        context.fill(x, y + 9, x + 10, y + 10, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + 10, 0xFFFFFFFF);
        context.fill(x + 9, y, x + 10, y + 10, 0xFFFFFFFF);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }
}