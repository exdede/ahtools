package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.core.HeldStack;
import dev.exdede.ahtools.core.HotbarScan;

import java.util.Map;
import java.util.Random;

/**
 * The seller's state machine.
 *
 * IDLE reads the live hotbar every tick and never remembers a slot list, since
 * a successful listing empties the slot it sold from. It only picks a slot
 * holding the preset's item; anything else in the hotbar is left alone.
 *
 * WAIT_GATE is where the preset's delay is served, and the command is built
 * only once it is over. "/ah sell" lists whatever is in hand at the moment
 * the server reads it, so the slot is checked again, selected, and the
 * command handed to the gate all in the one tick the gate sends it. Queuing
 * it earlier left a window of at least a second in which a scroll of the
 * mouse wheel would have listed a sword at the price of a map.
 *
 * WAIT_FOR_SALE deliberately never times out into STOPPED. A full listing book
 * with slow buyers is a normal market state, not a fault, and buyers arrive in
 * waves, so giving up after a few minutes would miss a flood that lands much
 * later. It wakes on either a sale line or a periodic re-probe. The re-probe is
 * required rather than optional because simultaneous sales get collapsed into
 * one aggregate line, so individual sale lines are missed exactly when the
 * market is moving fastest. One probe every few minutes carries no rate risk.
 *
 * STOPPED is only the "something is broken" path: a price the server refuses,
 * a price the local caps refuse, no /ah on this server, a hand the server keeps
 * calling empty, or no chat response at all after the retry.
 */
public final class SellCycle {
    public enum Phase { IDLE, WAIT_GATE, WAIT_RESULT, WAIT_FOR_SALE, STOPPED }

    /** How long to wait for any chat response after sending, in ticks (8 seconds). */
    public static final long RESULT_TIMEOUT_TICKS = 160L;
    /** How often to re-probe while parked on a full listing book, in ticks (3 minutes). */
    public static final long REPROBE_INTERVAL_TICKS = 3600L;
    public static final int NO_FEEDBACK_RETRY_LIMIT = 1;
    /** "You cannot sell air." this many times in a row means the hotbar and the server disagree. */
    public static final int EMPTY_HAND_LIMIT = 3;

    /**
     * What the gate is told. The preset's own delay has already been served
     * here before submitting, so the gate only has to hold its floor, and it
     * sends in the same tick.
     */
    private static final DelayRule SEND_NOW = DelayRule.fixed(DelayRule.MIN_DELAY_TICKS);

    private final SellEffects effects;
    private final Random rng;

    private boolean running;
    private Phase phase = Phase.IDLE;
    private Preset preset;
    private String lastError = "";

    private int currentSlot = -1;
    private String currentItemKey;
    private long currentPrice;
    private long waitTicks;
    private long cooldownUntilTick;
    private long phaseStartedAtTick;
    private long lastReprobeAtTick;
    private int noFeedbackRetries;
    private int emptyHandStreak;

    public SellCycle(SellEffects effects, Random rng) {
        this.effects = effects;
        this.rng = rng;
    }

    public boolean running() { return running; }
    public Phase phase() { return phase; }
    public String lastError() { return lastError; }
    public long currentPrice() { return currentPrice; }
    public int currentSlot() { return currentSlot; }
    public Preset preset() { return preset; }

    /**
     * Arms the cycle. Hotbar validation runs here, once, and only here: a
     * player who picks something up mid-run should not silently halt the
     * seller, which is why nothing re-validates during the run. Items that do
     * not match are skipped by IDLE instead.
     *
     * @return false when validation refused the start; see {@link #lastError()}.
     */
    public boolean start(Preset preset, long tick) {
        this.preset = preset;
        this.lastError = "";
        if (preset.validateHotbar()) {
            HotbarScan.Violation violation = HotbarScan.validate(effects.readHotbar(), preset.expectedItem());
            if (violation != null) {
                lastError = "hotbar slot " + violation.slot() + " holds " + violation.itemKey()
                    + ", expected " + preset.expectedItem();
                effects.log("ERROR", lastError);
                running = false;
                phase = Phase.IDLE;
                return false;
            }
        }
        running = true;
        phase = Phase.IDLE;
        currentSlot = -1;
        currentItemKey = null;
        currentPrice = 0L;
        noFeedbackRetries = 0;
        emptyHandStreak = 0;
        cooldownUntilTick = 0L;
        phaseStartedAtTick = tick;
        effects.log("STATE", "seller started with preset " + preset.name());
        return true;
    }

    public void stop() {
        running = false;
        phase = Phase.IDLE;
        currentSlot = -1;
        currentItemKey = null;
        noFeedbackRetries = 0;
        emptyHandStreak = 0;
        effects.log("STATE", "seller stopped");
    }

    public void tick(long tick) {
        if (!running || preset == null) return;
        switch (phase) {
            case IDLE -> tickIdle(tick);
            case WAIT_GATE -> tickWaitGate(tick);
            case WAIT_RESULT -> tickWaitResult(tick);
            case WAIT_FOR_SALE -> tickWaitForSale(tick);
            case STOPPED -> { }
        }
    }

    private void tickIdle(long tick) {
        Map<Integer, HeldStack> hotbar = effects.readHotbar();
        Integer slot = HotbarScan.firstMatchingSlot(hotbar, preset.expectedItem());
        if (slot == null) return;
        Long price = PricingStrategy.resolve(preset, rng);
        if (price == null) return;
        currentSlot = slot;
        currentItemKey = hotbar.get(slot).itemKey();
        currentPrice = price;
        noFeedbackRetries = 0;
        effects.log("ITEM", "picked hotbar slot " + slot + " (" + currentItemKey + ") at " + price);
        enterWaitGate(tick);
    }

    private void enterWaitGate(long tick) {
        waitTicks = preset.delayRule().nextDelayTicks(rng);
        phase = Phase.WAIT_GATE;
        phaseStartedAtTick = tick;
    }

    private void tickWaitGate(long tick) {
        if (effects.ticksSinceLastSend(tick) < waitTicks) return;
        if (tick < cooldownUntilTick) return;

        HeldStack held = effects.readHotbar().get(currentSlot);
        if (held == null || held.amount() <= 0 || !held.itemKey().equals(currentItemKey)) {
            effects.log("ITEM", "slot " + currentSlot + " changed before sending, picking again");
            phase = Phase.IDLE;
            return;
        }

        String command;
        try {
            command = SellCommand.sell(currentPrice, preset.maxPriceCap());
        }
        catch (IllegalArgumentException e) {
            stopWithError("refusing to send: " + e.getMessage());
            return;
        }
        effects.switchToSlot(currentSlot);
        if (!effects.submit(command, SEND_NOW)) return;
        effects.log("COMMAND", command);
        phase = Phase.WAIT_RESULT;
        phaseStartedAtTick = tick;
    }

    private void tickWaitResult(long tick) {
        if (tick - phaseStartedAtTick <= RESULT_TIMEOUT_TICKS) return;
        noFeedbackRetries++;
        if (noFeedbackRetries > NO_FEEDBACK_RETRY_LIMIT) {
            stopWithError("no chat response after " + noFeedbackRetries + " attempt(s)");
            return;
        }
        effects.log("ERROR", "no chat response, retrying");
        enterWaitGate(tick);
    }

    private void tickWaitForSale(long tick) {
        if (tick - lastReprobeAtTick < REPROBE_INTERVAL_TICKS) return;
        lastReprobeAtTick = tick;
        effects.log("STATE", "re-probing a full listing book");
        enterWaitGate(tick);
        tickWaitGate(tick);
    }

    private void stopWithError(String message) {
        lastError = message;
        effects.log("ERROR", message);
        phase = Phase.STOPPED;
        running = false;
    }

    /** Fed every chat line while the mod runs. Ignored unless the cycle is waiting on one. */
    public void onChatLine(String line, long tick) {
        if (!running) return;
        ChatSignals.Kind kind = ChatSignals.classify(line);
        if (kind == ChatSignals.Kind.NONE) return;

        if (phase == Phase.WAIT_RESULT) {
            switch (kind) {
                case LISTINGS_FULL -> {
                    effects.log("CHAT", line);
                    emptyHandStreak = 0;
                    phase = Phase.WAIT_FOR_SALE;
                    lastReprobeAtTick = tick;
                }
                case LISTED_OK -> {
                    effects.log("CHAT", line);
                    emptyHandStreak = 0;
                    phase = Phase.IDLE;
                }
                // A sale of an older listing says nothing about the one in
                // flight, so the wait for its own answer carries on.
                case SALE -> effects.log("CHAT", line);
                case CANNOT_SELL_AIR -> {
                    effects.log("CHAT", line);
                    emptyHandStreak++;
                    if (emptyHandStreak >= EMPTY_HAND_LIMIT) {
                        stopWithError("the server keeps saying the hand is empty");
                    }
                    else {
                        phase = Phase.IDLE;
                    }
                }
                case COOLDOWN -> {
                    effects.log("CHAT", line);
                    cooldownUntilTick = tick + ChatSignals.cooldownTicks(line);
                    phase = Phase.WAIT_GATE;
                    waitTicks = 0L;
                    phaseStartedAtTick = tick;
                }
                case INVALID_PRICE -> stopWithError(line);
                case COMMAND_UNAVAILABLE -> stopWithError("/ah is not available here: " + line);
                default -> { }
            }
        }
        else if (phase == Phase.WAIT_FOR_SALE) {
            switch (kind) {
                case SALE, LISTED_OK -> {
                    effects.log("CHAT", line);
                    phase = Phase.IDLE;
                }
                case LISTINGS_FULL -> lastReprobeAtTick = tick;
                default -> { }
            }
        }
    }
}
