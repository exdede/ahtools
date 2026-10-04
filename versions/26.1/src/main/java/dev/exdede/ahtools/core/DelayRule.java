package dev.exdede.ahtools.core;

import java.util.Random;

/**
 * How long to wait between two automated messages, in client ticks.
 *
 * The 20 tick floor is clamped here, at construction, and clamped again in
 * ChatSendGate before every send. That duplication is deliberate: a config
 * file edited by hand is not a trusted input, and the requirement is that the
 * floor holds at the sending layer rather than only in whatever GUI happened
 * to build the rule.
 */
public final class DelayRule {
    public static final int MIN_DELAY_TICKS = 20;

    public enum Mode { FIXED, RANDOM }

    private final Mode mode;
    private final int fixedTicks;
    private final int minTicks;
    private final int maxTicks;

    private DelayRule(Mode mode, int fixedTicks, int minTicks, int maxTicks) {
        this.mode = mode;
        this.fixedTicks = fixedTicks;
        this.minTicks = minTicks;
        this.maxTicks = maxTicks;
    }

    public static DelayRule fixed(int ticks) {
        int clamped = Math.max(MIN_DELAY_TICKS, ticks);
        return new DelayRule(Mode.FIXED, clamped, clamped, clamped);
    }

    public static DelayRule random(int minTicks, int maxTicks) {
        int min = Math.max(MIN_DELAY_TICKS, minTicks);
        int max = Math.max(min, maxTicks);
        return new DelayRule(Mode.RANDOM, min, min, max);
    }

    /** A fresh independent roll per message. Repeats are expected; the range is never consumed. */
    public int nextDelayTicks(Random rng) {
        if (mode == Mode.FIXED || maxTicks <= minTicks) return fixedTicks;
        return minTicks + rng.nextInt(maxTicks - minTicks + 1);
    }

    public Mode mode() { return mode; }
    public int fixedTicks() { return fixedTicks; }
    public int minTicks() { return minTicks; }
    public int maxTicks() { return maxTicks; }
}
