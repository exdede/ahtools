package dev.exdede.ahtools.core;

import java.util.Locale;
import java.util.Map;

/**
 * Pure decisions over a hotbar snapshot. Empty slots are simply absent from
 * the map, so there is no "is air" check anywhere in the mod.
 *
 * The seller re-reads the hotbar on every idle tick rather than remembering a
 * slot list, because a successful listing empties the slot it sold from. That
 * is why this class only ever answers questions about a snapshot it was handed
 * and holds no state of its own.
 */
public final class HotbarScan {
    public static final int FIRST_SLOT = 0;
    public static final int LAST_SLOT = 8;

    /** An unexpected item found while validating, identified for the message shown to the player. */
    public record Violation(int slot, String itemKey) {}

    private HotbarScan() {}

    /** The lowest occupied hotbar index, or null when every slot is empty. */
    public static Integer firstNonEmptySlot(Map<Integer, HeldStack> hotbar) {
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            HeldStack held = hotbar.get(slot);
            if (held != null && held.amount() > 0) return slot;
        }
        return null;
    }

    /**
     * The lowest occupied slot holding expectedItem, or null when there is
     * none. A blank expectedItem means "anything", which is the same answer
     * as {@link #firstNonEmptySlot}. Slots holding something else are skipped
     * rather than sold: the seller must never list an item the preset does
     * not name.
     */
    public static Integer firstMatchingSlot(Map<Integer, HeldStack> hotbar, String expectedItem) {
        if (expectedItem == null || expectedItem.isBlank()) return firstNonEmptySlot(hotbar);
        String normalizedExpected = normalize(expectedItem);
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            HeldStack held = hotbar.get(slot);
            if (held != null && held.amount() > 0 && normalizedExpected.equals(held.itemKey())) return slot;
        }
        return null;
    }

    /**
     * Checks that every occupied hotbar slot holds expectedItem. Empty slots
     * are always fine. Returns the lowest offending slot, or null when the
     * hotbar passes or when no expected item is configured.
     */
    public static Violation validate(Map<Integer, HeldStack> hotbar, String expectedItem) {
        if (expectedItem == null || expectedItem.isBlank()) return null;
        String normalizedExpected = normalize(expectedItem);
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            HeldStack held = hotbar.get(slot);
            if (held == null || held.amount() <= 0) continue;
            if (!normalizedExpected.equals(held.itemKey())) return new Violation(slot, held.itemKey());
        }
        return null;
    }

    /**
     * itemKey always carries the registry namespace ("minecraft:filled_map"),
     * but a preset's expected item is free text a player typed by hand and
     * usually leaves the namespace off. Without this, a correctly filled
     * hotbar slot would be reported as a violation against itself. Registry
     * ids are always lower case, so case and stray spaces are dropped too.
     */
    private static String normalize(String itemId) {
        String clean = itemId.trim().toLowerCase(Locale.ROOT);
        return clean.indexOf(':') < 0 ? "minecraft:" + clean : clean;
    }
}
