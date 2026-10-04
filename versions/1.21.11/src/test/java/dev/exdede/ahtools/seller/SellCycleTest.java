package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.core.HeldStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SellCycleTest {
    private final Map<Integer, HeldStack> hotbar = new HashMap<>();
    private final List<String> sent = new ArrayList<>();
    private final List<Integer> switched = new ArrayList<>();
    private boolean gateAccepts = true;
    private long sinceLastSend = 1_000_000L;

    private final SellEffects effects = new SellEffects() {
        @Override public Map<Integer, HeldStack> readHotbar() { return new HashMap<>(hotbar); }
        @Override public long ticksSinceLastSend(long tick) { return sinceLastSend; }
        @Override public void switchToSlot(int hotbarSlot) { switched.add(hotbarSlot); }
        @Override public boolean submit(String command, DelayRule rule) {
            if (!gateAccepts) return false;
            sent.add(command);
            return true;
        }
        @Override public void log(String kind, String message) { }
    };

    private final SellCycle cycle = new SellCycle(effects, new Random(5));

    private Preset preset(boolean validate, String expected) {
        return new Preset("Observer", expected, Preset.PricingMode.FIXED, 5000L, 0L, 0L, 0L,
            1_000_000L, DelayRule.Mode.FIXED, 25, 25, 40, 1500L, validate);
    }

    private void fill(int slot, String item) {
        hotbar.put(slot, new HeldStack(item, "Observer", 64));
    }

    @BeforeEach
    void reset() {
        hotbar.clear();
        sent.clear();
        switched.clear();
        gateAccepts = true;
        sinceLastSend = 1_000_000L;
    }

    @Test
    void startRefusesWhenValidationFindsAnUnexpectedItem() {
        fill(0, "minecraft:observer");
        fill(2, "minecraft:dirt");
        assertFalse(cycle.start(preset(true, "minecraft:observer"), 0L));
        assertFalse(cycle.running());
        assertTrue(cycle.lastError().contains("slot 2"), cycle.lastError());
        assertTrue(cycle.lastError().contains("minecraft:dirt"), cycle.lastError());
    }

    @Test
    void startAcceptsEmptySlotsDuringValidation() {
        fill(0, "minecraft:observer");
        fill(3, "minecraft:observer");
        assertTrue(cycle.start(preset(true, "minecraft:observer"), 0L));
        assertTrue(cycle.running());
    }

    @Test
    void startSkipsValidationWhenDisabled() {
        fill(0, "minecraft:dirt");
        assertTrue(cycle.start(preset(false, "minecraft:observer"), 0L));
    }

    @Test
    void idlePicksTheSlotAndTheSendTickSelectsIt() {
        fill(1, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        assertEquals(SellCycle.Phase.WAIT_GATE, cycle.phase());
        assertTrue(switched.isEmpty(), "selected the slot before the send tick");

        cycle.tick(1L);
        assertEquals(List.of(1), switched);
        assertEquals(List.of("/ah sell 5000"), sent);
        assertEquals(SellCycle.Phase.WAIT_RESULT, cycle.phase());
    }

    @Test
    void onlySlotsHoldingThePresetItemAreSold() {
        hotbar.put(0, new HeldStack("minecraft:diamond_sword", "Diamond Sword", 1));
        fill(1, "minecraft:observer");
        cycle.start(preset(false, "observer"), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        assertEquals(List.of(1), switched);

        hotbar.remove(1);
        cycle.onChatLine("You listed 64 Observer for $ 5K", 2L);
        for (long tick = 3; tick < 100; tick++) cycle.tick(tick);
        assertEquals(1, sent.size(), "listed the sword");
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
    }

    @Test
    void anItemSwappedBeforeTheSendTickIsNeverSold() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        hotbar.put(0, new HeldStack("minecraft:diamond_sword", "Diamond Sword", 1));
        cycle.tick(1L);
        assertTrue(sent.isEmpty());
        assertTrue(switched.isEmpty());
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
    }

    @Test
    void thePresetDelayIsServedBeforeSubmitting() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        sinceLastSend = 24L;
        cycle.tick(1L);
        assertTrue(sent.isEmpty(), "sent before the preset's 25 tick delay");
        sinceLastSend = 25L;
        cycle.tick(2L);
        assertEquals(1, sent.size());
    }

    @Test
    void staysInWaitGateWhileTheGateRefuses() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        gateAccepts = false;
        for (long tick = 1; tick < 50; tick++) cycle.tick(tick);
        assertEquals(SellCycle.Phase.WAIT_GATE, cycle.phase());
        assertTrue(sent.isEmpty());

        gateAccepts = true;
        cycle.tick(50L);
        assertEquals(SellCycle.Phase.WAIT_RESULT, cycle.phase());
    }

    @Test
    void listedOkRescansImmediately() {
        fill(0, "minecraft:observer");
        fill(1, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        hotbar.remove(0);
        cycle.onChatLine("You listed 64 Observer for $ 5K", 2L);
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());

        cycle.tick(3L);
        cycle.tick(4L);
        assertEquals(List.of(0, 1), switched);
    }

    @Test
    void listingsFullParksAndTheReprobeWakesIt() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("You have too many listed items!", 2L);
        assertEquals(SellCycle.Phase.WAIT_FOR_SALE, cycle.phase());

        cycle.tick(100L);
        assertEquals(1, sent.size(), "re-probed too early");

        cycle.tick(2L + SellCycle.REPROBE_INTERVAL_TICKS);
        assertEquals(2, sent.size(), "re-probe never fired");
        assertEquals(SellCycle.Phase.WAIT_RESULT, cycle.phase());
    }

    @Test
    void waitForSaleNeverStopsOnItsOwn() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("You have too many listed items!", 2L);
        for (long tick = 3; tick < 3 + SellCycle.REPROBE_INTERVAL_TICKS - 1; tick++) cycle.tick(tick);
        assertNotEquals(SellCycle.Phase.STOPPED, cycle.phase());
    }

    @Test
    void saleWhileParkedGoesBackToScanning() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("You have too many listed items!", 2L);
        cycle.onChatLine("Olrun bought your Observer for $5K", 3L);
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
    }

    @Test
    void invalidPriceStopsImmediately() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("That is not a valid number.", 2L);
        assertEquals(SellCycle.Phase.STOPPED, cycle.phase());
        assertFalse(cycle.running());
        assertFalse(cycle.lastError().isEmpty());
    }

    @Test
    void noFeedbackRetriesOnceThenStops() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        assertEquals(1, sent.size());

        cycle.tick(1L + SellCycle.RESULT_TIMEOUT_TICKS + 1);
        assertEquals(SellCycle.Phase.WAIT_GATE, cycle.phase());
        cycle.tick(1L + SellCycle.RESULT_TIMEOUT_TICKS + 2);
        assertEquals(2, sent.size(), "did not retry");

        long base = 1L + SellCycle.RESULT_TIMEOUT_TICKS + 2;
        cycle.tick(base + SellCycle.RESULT_TIMEOUT_TICKS + 1);
        assertEquals(SellCycle.Phase.STOPPED, cycle.phase());
        assertFalse(cycle.running());
    }

    @Test
    void anEmptyHotbarJustWaits() {
        cycle.start(preset(false, ""), 0L);
        for (long tick = 0; tick < 100; tick++) cycle.tick(tick);
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
        assertTrue(sent.isEmpty());
    }

    @Test
    void anUnpriceablePresetNeverSends() {
        fill(0, "minecraft:observer");
        Preset broken = new Preset("Broken", "", Preset.PricingMode.RANDOM_RANGE, 0L, 5000L, 1000L, 100L,
            1_000_000L, DelayRule.Mode.FIXED, 25, 25, 40, 0L, false);
        cycle.start(broken, 0L);
        for (long tick = 0; tick < 50; tick++) cycle.tick(tick);
        assertTrue(sent.isEmpty());
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
    }

    @Test
    void aPriceOverThePresetCapStops() {
        fill(0, "minecraft:observer");
        Preset overCap = new Preset("Over", "", Preset.PricingMode.FIXED, 50_000L, 0L, 0L, 0L,
            10_000L, DelayRule.Mode.FIXED, 25, 25, 40, 0L, false);
        cycle.start(overCap, 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        assertEquals(SellCycle.Phase.STOPPED, cycle.phase());
        assertTrue(sent.isEmpty());
    }

    @Test
    void stopHaltsEverythingAndTicksBecomeNoOps() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.stop();
        assertFalse(cycle.running());
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
        for (long tick = 1; tick < 50; tick++) cycle.tick(tick);
        assertTrue(sent.isEmpty());
    }

    @Test
    void chatIsIgnoredWhileNotRunning() {
        cycle.onChatLine("That is not a valid number.", 1L);
        assertEquals(SellCycle.Phase.IDLE, cycle.phase());
    }

    @Test
    void aSaleOfAnOlderListingDoesNotEndTheWait() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("Olrun bought your Observer for $5K", 2L);
        assertEquals(SellCycle.Phase.WAIT_RESULT, cycle.phase());
    }

    @Test
    void anEmptyHandPicksAgainAndRepeatedEmptyHandsStop() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        long tick = 0L;
        for (int round = 1; round <= SellCycle.EMPTY_HAND_LIMIT; round++) {
            cycle.tick(tick++);
            cycle.tick(tick++);
            assertEquals(SellCycle.Phase.WAIT_RESULT, cycle.phase());
            cycle.onChatLine("You cannot sell air.", tick++);
            if (round < SellCycle.EMPTY_HAND_LIMIT) assertEquals(SellCycle.Phase.IDLE, cycle.phase());
        }
        assertEquals(SellCycle.Phase.STOPPED, cycle.phase());
        assertFalse(cycle.running());
    }

    @Test
    void aCooldownWaitsTheServersTimeAndDoesNotCountAsAFailure() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("You need to wait another 2.5 seconds to execute a command", 2L);
        assertEquals(SellCycle.Phase.WAIT_GATE, cycle.phase());
        for (long tick = 3; tick < 52; tick++) cycle.tick(tick);
        assertEquals(1, sent.size(), "retried inside the server's cooldown");
        cycle.tick(52L);
        assertEquals(2, sent.size());

        // One no-response retry is still left after the cooldown.
        cycle.tick(52L + SellCycle.RESULT_TIMEOUT_TICKS + 1);
        assertEquals(SellCycle.Phase.WAIT_GATE, cycle.phase());
    }

    @Test
    void noAuctionCommandOnThisServerStops() {
        fill(0, "minecraft:observer");
        cycle.start(preset(false, ""), 0L);
        cycle.tick(0L);
        cycle.tick(1L);
        cycle.onChatLine("This command does not exist", 2L);
        assertEquals(SellCycle.Phase.STOPPED, cycle.phase());
    }
}
