package com.sb_sub.tradescanner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradeMarketScanner {
    private static final boolean DEBUG_LOGGING = false;

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(?:(?<amount>\\d+(?:,\\d+)*)\\s*x\\s*)?(?<price>\\d+(?:,\\d+)*)");

    private static final Path LOG_FILE = Path.of("tradescanner-log.txt");
    private static final Map<Integer, HighlightType> highlightedSlots = new HashMap<>();

    public static void scan(MinecraftClient client) {
        highlightedSlots.clear();

        if (client.player == null || client.player.currentScreenHandler == null) {
            log("No open container.");
            return;
        }

        clearLog();

        PlayerInventory playerInventory = client.player.getInventory();

        log("Scanning open container...");
        log("Screen handler: " + client.player.currentScreenHandler.getClass().getName());

        List<SlotPrice> prices = new ArrayList<>();

        for (Slot slot : client.player.currentScreenHandler.slots) {
            if (slot.inventory == playerInventory) {
                continue;
            }

            if (slot.id >= 45 && slot.id <= 53) {
                continue;
            }

            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) {
                continue;
            }

            int price = getPrice(stack);

            log(
                    "Slot " + slot.id
                            + " | Item: " + stack.getName().getString()
                            + " | Price: " + price
            );

            printLore(stack);

            if (price >= 0) {
                prices.add(new SlotPrice(slot.id, stack.getName().getString(), price));
            }
        }

        log("Found " + prices.size() + " priced listings.");

        if (prices.isEmpty()) {
            return;
        }

        applyHighlights(prices);
        logHighlights(prices);
    }

    public static HighlightType getHighlightType(int slotId) {
        return highlightedSlots.get(slotId);
    }

    public static boolean hasHighlights() {
        return !highlightedSlots.isEmpty();
    }

    public static void clearHighlights() {
        highlightedSlots.clear();
    }

    private static void applyHighlights(List<SlotPrice> prices) {
        int lowestPrice = prices.stream()
                .mapToInt(SlotPrice::price)
                .min()
                .orElse(-1);

        int activeSbThreshold = -1;

        for (int i = 0; i < prices.size(); i++) {
            SlotPrice current = prices.get(i);

            if (current.price == lowestPrice) {
                highlightedSlots.put(current.slotId, HighlightType.LBO);
            } else if (activeSbThreshold >= 0 && current.price < activeSbThreshold) {
                highlightedSlots.put(current.slotId, HighlightType.HAS_SB);
            } else {
                highlightedSlots.put(current.slotId, HighlightType.NO_SB);
            }

            if (i < prices.size() - 1) {
                SlotPrice next = prices.get(i + 1);

                if (current.price > next.price) {
                    activeSbThreshold = current.price;

                    log(
                            "SB threshold activated after slot "
                                    + current.slotId
                                    + ": "
                                    + current.price
                                    + " > "
                                    + next.price
                    );
                }
            }
        }
    }

    private static void logHighlights(List<SlotPrice> prices) {
        for (SlotPrice listing : prices) {
            log(
                    "Highlight: slot "
                            + listing.slotId
                            + " | "
                            + listing.itemName
                            + " | "
                            + listing.price
                            + " | "
                            + highlightedSlots.get(listing.slotId)
            );
        }
    }

    private static int getPrice(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);

        if (lore == null) {
            return -1;
        }

        for (Text line : lore.lines()) {
            String clean = line.getString();

            Matcher matcher = PRICE_PATTERN.matcher(clean);

            if (matcher.find()) {
                String priceText = matcher.group("price").replace(",", "");
                return Integer.parseInt(priceText);
            }
        }

        return -1;
    }

    private static void printLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);

        if (lore == null) {
            return;
        }

        for (Text line : lore.lines()) {
            log("  Lore: " + line.getString());
        }
    }

    private static void clearLog() {
        if (!DEBUG_LOGGING) {
            return;
        }

        try {
            Files.writeString(LOG_FILE, "");
        } catch (IOException e) {
            System.out.println("[TradeScanner] Failed to clear log: " + e.getMessage());
        }
    }

    private static void log(String message) {
        if (!DEBUG_LOGGING) {
            return;
        }

        try {
            Files.writeString(
                    LOG_FILE,
                    message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.out.println("[TradeScanner] Failed to write log: " + e.getMessage());
        }
    }

    public enum HighlightType {
        HAS_SB,
        NO_SB,
        LBO
    }

    private record SlotPrice(int slotId, String itemName, int price) {}
}