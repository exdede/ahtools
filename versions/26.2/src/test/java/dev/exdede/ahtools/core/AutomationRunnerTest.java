package dev.exdede.ahtools.core;

import dev.exdede.ahtools.seller.Preset;
import dev.exdede.ahtools.seller.SellCycle;
import dev.exdede.ahtools.seller.SellEffects;
import dev.exdede.ahtools.spammer.SpamController;
import dev.exdede.ahtools.spammer.SpamEffects;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AutomationRunnerTest {
    private final List<String> sent = new ArrayList<>();
    private final Map<Integer, HeldStack> hotbar = new HashMap<>();
    private final ChatSendGate gate = new ChatSendGate(sent::add, new Random(3));

    private final SellEffects sellEffects = new SellEffects() {
        @Override public Map<Integer, HeldStack> readHotbar() { return new HashMap<>(hotbar); }
        @Override public long ticksSinceLastSend(long tick) { return gate.ticksSinceLastSend(tick); }
        @Override public void switchToSlot(int hotbarSlot) { }
        @Override public boolean submit(String command, DelayRule rule) { return gate.submit(command, "seller", rule); }
        @Override public void log(String kind, String message) { }
    };

    private final SpamEffects spamEffects = new SpamEffects() {
        @Override public long ticksSinceLastSend(long tick) { return gate.ticksSinceLastSend(tick); }
        @Override public boolean submit(String command, DelayRule rule) { return gate.submit(command, "spammer", rule); }
        @Override public void log(String kind, String message) { }
    };

    private final SellCycle seller = new SellCycle(sellEffects, new Random(4));
    private final SpamController spammer = new SpamController(spamEffects);
    private boolean consent = true;
    private final AutomationRunner runner = new AutomationRunner(gate, seller, spammer, () -> consent);

    private Preset preset() {
        return new Preset("Observer", "", Preset.PricingMode.FIXED, 5000L, 0L, 0L, 0L, 1_000_000L,
            DelayRule.Mode.FIXED, 20, 20, 20, 1500L, false);
    }

    @Test
    void togglingFlipsTheRunningState() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        assertTrue(runner.toggleSeller(preset(), 0L));
        assertTrue(runner.sellerRunning());
        assertFalse(runner.toggleSeller(preset(), 1L));
        assertFalse(runner.sellerRunning());
    }

    @Test
    void bothFeaturesShareOneGateSoTheRateNeverDoubles() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.toggleSpammer(List.of("/spam"), DelayRule.fixed(20), 0L);

        for (long tick = 0; tick <= 60; tick++) runner.tick(tick);
        assertTrue(sent.size() <= 4, "sent " + sent.size() + " messages in 60 ticks, gate not shared");
    }

    @Test
    void neitherFeatureStarvesTheOther() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.toggleSpammer(List.of("/spam"), DelayRule.fixed(20), 0L);
        for (long tick = 0; tick <= 200; tick++) {
            runner.tick(tick);
            // Answer every listing at once, so the seller is always ready again.
            if (seller.phase() == SellCycle.Phase.WAIT_RESULT) runner.onChatLine("You listed 64 Observer for $ 5K", tick);
        }
        long sells = sent.stream().filter(m -> m.startsWith("/ah sell")).count();
        long spams = sent.stream().filter(m -> m.equals("/spam")).count();
        assertTrue(sells >= 3, "seller starved: " + sent);
        assertTrue(spams >= 3, "spammer starved: " + sent);
    }

    @Test
    void aLongPresetDelaySendsOnceAndTimesOutFromTheRealSend() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        hotbar.put(1, new HeldStack("minecraft:observer", "Observer", 64));
        Preset slow = new Preset("Slow", "observer", Preset.PricingMode.FIXED, 5000L, 0L, 0L, 0L, 1_000_000L,
            DelayRule.Mode.FIXED, 400, 400, 400, 0L, false);
        runner.toggleSeller(slow, 0L);
        runner.tick(0L);
        runner.tick(1L);
        assertEquals(1, sent.size());
        hotbar.remove(0);
        runner.onChatLine("You listed 64 Observer for $ 5K", 5L);

        // The second listing waits out the 400 ticks with no timeout and no duplicate.
        for (long tick = 6; tick <= 400; tick++) runner.tick(tick);
        assertEquals(1, sent.size(), "sent before the preset delay: " + sent);
        runner.tick(401L);
        assertEquals(2, sent.size());
        assertFalse(gate.hasPending());

        // No answer: one retry 400 ticks after the real send, then a stop, and nothing after it.
        for (long tick = 402; tick <= 3000; tick++) runner.tick(tick);
        assertEquals(3, sent.size(), "expected one retry: " + sent);
        assertEquals(SellCycle.Phase.STOPPED, seller.phase());
        assertFalse(gate.hasPending());
    }

    @Test
    void emergencyStopHaltsBothAndDropsThePendingMessage() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.toggleSpammer(List.of("/spam"), DelayRule.fixed(20), 0L);
        runner.tick(0L);
        runner.tick(1L);
        sent.clear();

        runner.emergencyStop();
        assertFalse(runner.sellerRunning());
        assertFalse(runner.spammerRunning());
        assertFalse(gate.hasPending());

        for (long tick = 2; tick < 200; tick++) runner.tick(tick);
        assertTrue(sent.isEmpty(), "sent " + sent + " after the emergency stop");
    }

    @Test
    void disconnectStopsEverythingTheSameWay() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.toggleSpammer(List.of("/spam"), DelayRule.fixed(20), 0L);
        runner.tick(0L);
        sent.clear();

        runner.onDisconnect();
        assertFalse(runner.sellerRunning());
        assertFalse(runner.spammerRunning());
        for (long tick = 1; tick < 200; tick++) runner.tick(tick);
        assertTrue(sent.isEmpty());
    }

    @Test
    void nothingResumesByItselfAfterAStop() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.onDisconnect();
        for (long tick = 1; tick < 500; tick++) runner.tick(tick);
        assertFalse(runner.sellerRunning());
    }

    @Test
    void chatLinesReachTheSeller() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        runner.toggleSeller(preset(), 0L);
        runner.tick(0L);
        runner.tick(1L);
        runner.onChatLine("That is not a valid number.", 2L);
        assertEquals(SellCycle.Phase.STOPPED, seller.phase());
    }

    @Test
    void sellerRefusesToStartWithoutConsent() {
        consent = false;
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        assertFalse(runner.toggleSeller(preset(), 0L));
        assertFalse(runner.sellerRunning());
    }

    @Test
    void spammerRefusesToStartWithoutConsent() {
        consent = false;
        assertFalse(runner.toggleSpammer(List.of("/spam"), DelayRule.fixed(20), 0L));
        assertFalse(runner.spammerRunning());
    }

    @Test
    void withdrawnConsentStillLetsARunningSellerStop() {
        hotbar.put(0, new HeldStack("minecraft:observer", "Observer", 64));
        assertTrue(runner.toggleSeller(preset(), 0L));
        consent = false;
        assertFalse(runner.toggleSeller(preset(), 1L));
        assertFalse(runner.sellerRunning());
    }
}
