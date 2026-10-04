package dev.exdede.ahtools.spammer;

import dev.exdede.ahtools.core.DelayRule;

/**
 * Everything the spammer can do to the outside world. Same shape as the
 * seller's effects interface, and pointed at the same send gate, which is what
 * stops two running features from doubling the outgoing message rate.
 */
public interface SpamEffects {
    /** Ticks since the shared gate last sent anything, from either feature. */
    long ticksSinceLastSend(long tick);

    /** Queues a command on the shared send gate. False means the gate is busy. */
    boolean submit(String command, DelayRule rule);

    void log(String kind, String message);
}
