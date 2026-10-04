package dev.exdede.ahtools.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class DelayRuleTest {
    @Test
    void fixedClampsBelowTheFloor() {
        DelayRule rule = DelayRule.fixed(3);
        assertEquals(DelayRule.MIN_DELAY_TICKS, rule.fixedTicks());
        assertEquals(20, rule.nextDelayTicks(new Random(1)));
    }

    @Test
    void fixedKeepsValuesAboveTheFloor() {
        assertEquals(25, DelayRule.fixed(25).nextDelayTicks(new Random(1)));
    }

    @Test
    void randomClampsMinimumAndOrdersBounds() {
        DelayRule rule = DelayRule.random(5, 40);
        assertEquals(20, rule.minTicks());
        assertEquals(40, rule.maxTicks());

        DelayRule inverted = DelayRule.random(60, 30);
        assertEquals(60, inverted.minTicks());
        assertEquals(60, inverted.maxTicks());
    }

    @Test
    void randomStaysInRangeAndVaries() {
        DelayRule rule = DelayRule.random(20, 40);
        Random rng = new Random(42);
        boolean sawDifferent = false;
        int first = rule.nextDelayTicks(rng);
        for (int i = 0; i < 200; i++) {
            int value = rule.nextDelayTicks(rng);
            assertTrue(value >= 20 && value <= 40, "out of range: " + value);
            if (value != first) sawDifferent = true;
        }
        assertTrue(sawDifferent, "random delay never varied");
    }
}
