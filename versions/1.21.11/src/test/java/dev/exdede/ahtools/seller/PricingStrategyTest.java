package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.core.DelayRule;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTest {
    private Preset priced(Preset.PricingMode mode, long fixed, long min, long max, long step) {
        return new Preset("P", "", mode, fixed, min, max, step, 100_000_000L,
            DelayRule.Mode.FIXED, 25, 25, 40, 0L, false);
    }

    @Test
    void fixedIgnoresStackSizeEntirely() {
        Preset preset = priced(Preset.PricingMode.FIXED, 10_000L, 0L, 0L, 0L);
        assertEquals(10_000L, PricingStrategy.resolve(preset, new Random(1)));
    }

    @Test
    void randomRangeSnapsToStepAndStaysInBounds() {
        Preset preset = priced(Preset.PricingMode.RANDOM_RANGE, 0L, 3_000L, 20_000L, 1_000L);
        Random rng = new Random(11);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            long price = PricingStrategy.resolve(preset, rng);
            assertTrue(price >= 3_000L && price <= 20_000L, "out of range: " + price);
            assertEquals(0L, (price - 3_000L) % 1_000L, "not on a step: " + price);
            seen.add(price);
        }
        assertTrue(seen.contains(3_000L), "never produced the minimum");
        assertTrue(seen.contains(20_000L), "never produced the maximum");
    }

    @Test
    void randomRangeHandlesAMaximumThatIsNotOnAStep() {
        Preset preset = priced(Preset.PricingMode.RANDOM_RANGE, 0L, 1_000L, 2_500L, 1_000L);
        Random rng = new Random(3);
        for (int i = 0; i < 100; i++) {
            long price = PricingStrategy.resolve(preset, rng);
            assertTrue(price == 1_000L || price == 2_000L, "unexpected price: " + price);
        }
    }

    @Test
    void refusesImpossibleRanges() {
        assertNull(PricingStrategy.resolve(priced(Preset.PricingMode.RANDOM_RANGE, 0L, 5_000L, 1_000L, 100L), new Random(1)));
        assertNull(PricingStrategy.resolve(priced(Preset.PricingMode.RANDOM_RANGE, 0L, 1_000L, 5_000L, 0L), new Random(1)));
        assertNull(PricingStrategy.resolve(priced(Preset.PricingMode.RANDOM_RANGE, 0L, 0L, 5_000L, 100L), new Random(1)));
    }

    @Test
    void refusesANonPositiveFixedPrice() {
        assertNull(PricingStrategy.resolve(priced(Preset.PricingMode.FIXED, 0L, 0L, 0L, 0L), new Random(1)));
    }
}
