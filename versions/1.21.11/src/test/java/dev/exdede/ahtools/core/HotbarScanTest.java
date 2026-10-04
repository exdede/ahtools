package dev.exdede.ahtools.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HotbarScanTest {
    private Map<Integer, HeldStack> hotbar(Object... slotThenItem) {
        Map<Integer, HeldStack> map = new HashMap<>();
        for (int i = 0; i < slotThenItem.length; i += 2) {
            int slot = (Integer) slotThenItem[i];
            String item = (String) slotThenItem[i + 1];
            map.put(slot, new HeldStack(item, "Display", 64));
        }
        return map;
    }

    @Test
    void picksTheLowestNonEmptySlot() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:observer", 1, "minecraft:observer",
            3, "minecraft:observer");
        assertEquals(0, HotbarScan.firstNonEmptySlot(hotbar));
    }

    @Test
    void skipsEmptySlots() {
        Map<Integer, HeldStack> hotbar = hotbar(2, "minecraft:observer", 5, "minecraft:observer");
        assertEquals(2, HotbarScan.firstNonEmptySlot(hotbar));
    }

    @Test
    void returnsNullWhenTheHotbarIsEmpty() {
        assertNull(HotbarScan.firstNonEmptySlot(Map.of()));
    }

    @Test
    void ignoresSlotsOutsideTheHotbar() {
        Map<Integer, HeldStack> hotbar = hotbar(9, "minecraft:observer", 36, "minecraft:observer");
        assertNull(HotbarScan.firstNonEmptySlot(hotbar));
    }

    @Test
    void validationAcceptsMatchingItemsAndEmptySlots() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:observer", 1, "minecraft:observer",
            3, "minecraft:observer");
        assertNull(HotbarScan.validate(hotbar, "minecraft:observer"));
    }

    @Test
    void validationNamesTheOffendingSlotAndItem() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:observer", 2, "minecraft:dirt",
            3, "minecraft:observer");
        HotbarScan.Violation violation = HotbarScan.validate(hotbar, "minecraft:observer");
        assertNotNull(violation);
        assertEquals(2, violation.slot());
        assertEquals("minecraft:dirt", violation.itemKey());
    }

    @Test
    void validationReportsTheLowestOffendingSlotFirst() {
        Map<Integer, HeldStack> hotbar = hotbar(1, "minecraft:stone", 4, "minecraft:dirt");
        assertEquals(1, HotbarScan.validate(hotbar, "minecraft:observer").slot());
    }

    @Test
    void validationIsSkippedWithoutAnExpectedItem() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:dirt");
        assertNull(HotbarScan.validate(hotbar, ""));
        assertNull(HotbarScan.validate(hotbar, null));
    }

    @Test
    void validationAcceptsAnExpectedItemTypedWithoutNamespace() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:filled_map");
        assertNull(HotbarScan.validate(hotbar, "filled_map"));
    }

    @Test
    void matchingSlotSkipsOtherItems() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:diamond_sword", 1, "minecraft:filled_map");
        assertEquals(1, HotbarScan.firstMatchingSlot(hotbar, "filled_map"));
        assertNull(HotbarScan.firstMatchingSlot(hotbar(0, "minecraft:diamond_sword"), "filled_map"));
    }

    @Test
    void matchingSlotWithoutAnExpectedItemTakesAnything() {
        Map<Integer, HeldStack> hotbar = hotbar(2, "minecraft:diamond_sword");
        assertEquals(2, HotbarScan.firstMatchingSlot(hotbar, ""));
        assertEquals(2, HotbarScan.firstMatchingSlot(hotbar, null));
    }

    @Test
    void expectedItemIgnoresCaseAndSurroundingSpaces() {
        Map<Integer, HeldStack> hotbar = hotbar(0, "minecraft:filled_map");
        assertNull(HotbarScan.validate(hotbar, " Filled_Map "));
        assertEquals(0, HotbarScan.firstMatchingSlot(hotbar, "MINECRAFT:filled_map"));
    }
}
