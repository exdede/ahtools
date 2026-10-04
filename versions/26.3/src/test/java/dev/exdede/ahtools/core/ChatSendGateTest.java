package dev.exdede.ahtools.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ChatSendGateTest {
    private final List<String> sent = new ArrayList<>();
    private final ChatSendGate gate = new ChatSendGate(sent::add, new Random(7));

    @Test
    void firstMessageSendsImmediately() {
        assertTrue(gate.submit("/ah sell 5000", "seller", DelayRule.fixed(20)));
        gate.tick(0L);
        assertEquals(List.of("/ah sell 5000"), sent);
        assertFalse(gate.hasPending());
    }

    @Test
    void enforcesTwentyTicksEvenWhenTheRuleAsksForLess() {
        gate.submit("first", "seller", DelayRule.fixed(1));
        gate.tick(0L);
        assertEquals(1, sent.size());

        gate.submit("second", "seller", DelayRule.fixed(1));
        for (long tick = 1; tick < 20; tick++) gate.tick(tick);
        assertEquals(1, sent.size(), "sent before the 20 tick floor");

        gate.tick(20L);
        assertEquals(List.of("first", "second"), sent);
    }

    @Test
    void refusesASecondSubmitWhileOneIsPending() {
        gate.submit("first", "seller", DelayRule.fixed(20));
        gate.tick(0L);
        assertTrue(gate.submit("second", "seller", DelayRule.fixed(20)));
        assertFalse(gate.submit("third", "spammer", DelayRule.fixed(20)));
        gate.tick(20L);
        assertEquals(List.of("first", "second"), sent);
    }

    @Test
    void clearDropsThePendingMessage() {
        gate.submit("first", "seller", DelayRule.fixed(20));
        gate.tick(0L);
        gate.submit("second", "seller", DelayRule.fixed(20));
        assertTrue(gate.hasPending());
        gate.clear();
        assertFalse(gate.hasPending());
        for (long tick = 1; tick <= 100; tick++) gate.tick(tick);
        assertEquals(List.of("first"), sent);
    }

    @Test
    void reportsThePendingOrigin() {
        gate.submit("/spam", "spammer", DelayRule.fixed(20));
        assertEquals("spammer", gate.pendingOrigin());
    }

    @Test
    void rejectsBlankMessages() {
        assertFalse(gate.submit("   ", "seller", DelayRule.fixed(20)));
        assertFalse(gate.submit(null, "seller", DelayRule.fixed(20)));
        gate.tick(0L);
        assertTrue(sent.isEmpty());
    }
}
