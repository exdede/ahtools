package dev.exdede.ahtools.config.gui;

import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.seller.Preset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresetFieldTest {
    private final Preset base = Preset.defaults("Observer");

    @Test
    void acceptsSuffixedPricesTheWayChatWritesThem() {
        assertEquals(5000L, PresetField.apply(base, PresetField.Kind.FIXED_PRICE, "5k").fixedPrice());
        assertEquals(25900L, PresetField.apply(base, PresetField.Kind.FIXED_PRICE, "$25.9K").fixedPrice());
        assertEquals(1500L, PresetField.apply(base, PresetField.Kind.COST_PER_ITEM, "1,500").costPerItem());
    }

    @Test
    void rejectsUnusableValues() {
        assertThrows(IllegalArgumentException.class,
            () -> PresetField.apply(base, PresetField.Kind.FIXED_PRICE, "free"));
        assertThrows(IllegalArgumentException.class,
            () -> PresetField.apply(base, PresetField.Kind.FIXED_PRICE, "-5"));
        assertThrows(IllegalArgumentException.class,
            () -> PresetField.apply(base, PresetField.Kind.NAME, "   "));
    }

    @Test
    void clampsDelaysToTheFloorInsteadOfRefusingThem() {
        Preset edited = PresetField.apply(base, PresetField.Kind.FIXED_DELAY, "5");
        assertEquals(DelayRule.MIN_DELAY_TICKS, edited.fixedDelayTicks());
    }

    @Test
    void togglesCycleTheirValue() {
        assertEquals(Preset.PricingMode.RANDOM_RANGE,
            PresetField.apply(base, PresetField.Kind.PRICING_MODE, "").pricingMode());
        assertTrue(PresetField.apply(base, PresetField.Kind.VALIDATE_HOTBAR, "").validateHotbar());
        assertEquals(DelayRule.Mode.RANDOM,
            PresetField.apply(base, PresetField.Kind.DELAY_MODE, "").delayMode());
    }

    @Test
    void displaysMoneyFieldsInShorthand() {
        Preset preset = PresetField.apply(base, PresetField.Kind.FIXED_PRICE, "25900");
        assertEquals("$25.9K", PresetField.display(preset, PresetField.Kind.FIXED_PRICE));
        assertEquals("Observer", PresetField.display(preset, PresetField.Kind.NAME));
    }

    @Test
    void labelKeysFollowTheKindName() {
        assertEquals("ahtools.preset.fixed_price", PresetField.labelKey(PresetField.Kind.FIXED_PRICE));
        assertEquals("ahtools.preset.validate_hotbar", PresetField.labelKey(PresetField.Kind.VALIDATE_HOTBAR));
    }
}
