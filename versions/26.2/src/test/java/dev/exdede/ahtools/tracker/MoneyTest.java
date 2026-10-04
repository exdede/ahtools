package dev.exdede.ahtools.tracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void parsesPlainNumbers() {
        assertEquals(500L, Money.parse("$500"));
        assertEquals(67L, Money.parse("67"));
        assertEquals(51282707L, Money.parse("51,282,707"));
    }

    @Test
    void parsesSuffixes() {
        assertEquals(8000L, Money.parse("$8K"));
        assertEquals(25900L, Money.parse("$25.9K"));
        assertEquals(1000000L, Money.parse("$1M"));
        assertEquals(1500L, Money.parse("1.5k"));
        assertEquals(13800000L, Money.parse("~$ 13.8m"));
        assertEquals(2000000000L, Money.parse("$2B"));
    }

    @Test
    void toleratesSpacingAndCase() {
        assertEquals(6000L, Money.parse("$ 6K"));
        assertEquals(20000L, Money.parse("$ 20 K"));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Money.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Money.parse("$"));
        assertThrows(IllegalArgumentException.class, () -> Money.parse("free"));
        assertThrows(IllegalArgumentException.class, () -> Money.parse(null));
        assertNull(Money.parseOrNull("free"));
    }

    @Test
    void formatsForDisplay() {
        assertEquals("$500", Money.format(500L));
        assertEquals("$8K", Money.format(8000L));
        assertEquals("$25.9K", Money.format(25900L));
        assertEquals("$1.3M", Money.format(1300000L));
        assertEquals("$2.4B", Money.format(2400000000L));
        assertEquals("-$1K", Money.format(-1000L));
    }
}
