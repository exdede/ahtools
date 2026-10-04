package dev.exdede.ahtools.seller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SellCommandTest {
    @Test
    void buildsExactlyTheSellCommand() {
        assertEquals("/ah sell 5000", SellCommand.sell(5000L, 100_000L));
    }

    @Test
    void rejectsNonPositivePrices() {
        assertThrows(IllegalArgumentException.class, () -> SellCommand.sell(0L, 100_000L));
        assertThrows(IllegalArgumentException.class, () -> SellCommand.sell(-1L, 100_000L));
    }

    @Test
    void rejectsPricesOverThePresetCap() {
        assertThrows(IllegalArgumentException.class, () -> SellCommand.sell(200_000L, 100_000L));
    }

    @Test
    void rejectsPricesOverTheAbsoluteCeiling() {
        assertThrows(IllegalArgumentException.class,
            () -> SellCommand.sell(SellCommand.ABSOLUTE_MAX_PRICE + 1, Long.MAX_VALUE));
    }
}
