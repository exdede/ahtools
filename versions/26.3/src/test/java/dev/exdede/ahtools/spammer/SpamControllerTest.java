package dev.exdede.ahtools.spammer;

import dev.exdede.ahtools.core.DelayRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpamControllerTest {
    private final List<String> sent = new ArrayList<>();
    private boolean gateAccepts = true;
    private long sinceLastSend = 1_000_000L;

    private final SpamEffects effects = new SpamEffects() {
        @Override public long ticksSinceLastSend(long tick) { return sinceLastSend; }
        @Override public boolean submit(String command, DelayRule rule) {
            if (!gateAccepts) return false;
            sent.add(command);
            return true;
        }
        @Override public void log(String kind, String message) { }
    };

    private final SpamController controller = new SpamController(effects);

    @Test
    void sendsCommandsInOrderOneTickAtATime() {
        controller.start(List.of("/one", "/two"), DelayRule.fixed(20));
        controller.tick(0L);
        controller.tick(1L);
        controller.tick(2L);
        assertEquals(List.of("/one", "/two", "/one"), sent);
    }

    @Test
    void doesNotAdvanceWhileTheGateRefuses() {
        controller.start(List.of("/one", "/two"), DelayRule.fixed(20));
        gateAccepts = false;
        for (long tick = 0; tick < 20; tick++) controller.tick(tick);
        assertTrue(sent.isEmpty());

        gateAccepts = true;
        controller.tick(20L);
        controller.tick(21L);
        assertEquals(List.of("/one", "/two"), sent);
    }

    @Test
    void refusesToStartWithNoUsableCommands() {
        assertFalse(controller.start(List.of(), DelayRule.fixed(20)));
        assertFalse(controller.running());
        assertFalse(controller.lastError().isEmpty());
    }

    @Test
    void stopHaltsSendingAndResetsThePosition() {
        controller.start(List.of("/one", "/two"), DelayRule.fixed(20));
        controller.tick(0L);
        controller.stop();
        assertFalse(controller.running());
        for (long tick = 1; tick < 20; tick++) controller.tick(tick);
        assertEquals(List.of("/one"), sent);

        controller.start(List.of("/one", "/two"), DelayRule.fixed(20));
        controller.tick(20L);
        assertEquals(List.of("/one", "/one"), sent);
    }

    @Test
    void servesItsOwnDelayBeforeSubmitting() {
        controller.start(List.of("/one"), DelayRule.fixed(30));
        sinceLastSend = 29L;
        controller.tick(0L);
        assertTrue(sent.isEmpty(), "submitted before its own delay");
        sinceLastSend = 30L;
        controller.tick(1L);
        assertEquals(List.of("/one"), sent);
    }
}
