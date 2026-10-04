package dev.exdede.ahtools.core;

import java.util.Random;

/**
 * The single exit for every automated message this mod sends. Neither the
 * seller nor the spammer may talk to the network directly: they submit here
 * and the gate decides when the message actually leaves.
 *
 * One shared gate is what makes "at most one automated message per delay"
 * an architectural property rather than a per-feature promise. Two features
 * each honouring their own delay would still add up to twice the rate.
 *
 * The gate holds at most one pending message. A submit while one is pending
 * is refused, and the caller simply tries again on its next tick.
 */
public final class ChatSendGate {
    @FunctionalInterface
    public interface Sender {
        void send(String message);
    }

    private final Sender sender;
    private final Random rng;

    private String pending;
    private String pendingOrigin;
    private int pendingDelayTicks;
    // Far enough in the past that the very first submit is never held back by
    // a phantom previous send.
    private long lastSendTick = Long.MIN_VALUE / 2;
    private String lastSentOrigin;

    public ChatSendGate(Sender sender, Random rng) {
        this.sender = sender;
        this.rng = rng;
    }

    /**
     * Queues one message. The delay is rolled now, once, so a random rule
     * produces an independent wait per message rather than per tick.
     *
     * @return false when the message was blank or another message is already pending.
     */
    public boolean submit(String message, String origin, DelayRule rule) {
        if (message == null || message.isBlank()) return false;
        if (pending != null) return false;
        pending = message;
        pendingOrigin = origin;
        pendingDelayTicks = Math.max(DelayRule.MIN_DELAY_TICKS, rule.nextDelayTicks(rng));
        return true;
    }

    /** Call once per client tick with a monotonically increasing tick counter. */
    public void tick(long currentTick) {
        if (pending == null) return;
        if (currentTick - lastSendTick < pendingDelayTicks) return;
        String message = pending;
        lastSentOrigin = pendingOrigin;
        pending = null;
        pendingOrigin = null;
        lastSendTick = currentTick;
        sender.send(message);
    }

    /** Drops the pending message without sending it. Emergency stop and disconnect both call this. */
    public void clear() {
        pending = null;
        pendingOrigin = null;
    }

    public boolean hasPending() { return pending != null; }
    public String pendingOrigin() { return pendingOrigin; }
    public long lastSendTick() { return lastSendTick; }
    /** Who the last message that actually left belonged to, or null before the first send. */
    public String lastSentOrigin() { return lastSentOrigin; }
    public long ticksSinceLastSend(long currentTick) { return currentTick - lastSendTick; }
}
