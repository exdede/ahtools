package dev.exdede.ahtools.spammer;

import dev.exdede.ahtools.core.DelayRule;

import java.util.List;
import java.util.Random;

/**
 * Feeds the sequence into the send gate. There is no state machine here
 * because the spammer does not care what the server says back: it has no
 * success condition to detect and nothing to park on.
 *
 * The cursor only advances when the gate actually accepted the command, so a
 * busy gate delays the rotation instead of skipping entries in it.
 *
 * Like the seller, the spammer serves its own delay and only then submits, so
 * the gate sends in the same tick. Queuing early would let the spammer sit on
 * the gate's single slot permanently and starve the seller, which only ever
 * submits once its own delay is over.
 */
public final class SpamController {
    private static final DelayRule SEND_NOW = DelayRule.fixed(DelayRule.MIN_DELAY_TICKS);

    private final SpamEffects effects;
    private final Random rng;

    private SpamSequence sequence = new SpamSequence(List.of());
    private DelayRule rule = DelayRule.fixed(DelayRule.MIN_DELAY_TICKS);
    private boolean running;
    private String lastError = "";
    private long waitTicks = -1L;

    public SpamController(SpamEffects effects) {
        this(effects, new Random());
    }

    public SpamController(SpamEffects effects, Random rng) {
        this.effects = effects;
        this.rng = rng;
    }

    public boolean running() { return running; }
    public String lastError() { return lastError; }

    public boolean start(List<String> commands, DelayRule rule) {
        SpamSequence candidate = new SpamSequence(commands);
        if (candidate.commands().isEmpty()) {
            lastError = "no commands configured";
            effects.log("ERROR", lastError);
            running = false;
            return false;
        }
        this.sequence = candidate;
        this.rule = rule;
        this.lastError = "";
        this.waitTicks = -1L;
        this.running = true;
        effects.log("STATE", "spammer started with " + candidate.commands().size() + " command(s)");
        return true;
    }

    public void stop() {
        running = false;
        sequence.reset();
        effects.log("STATE", "spammer stopped");
    }

    public void tick(long tick) {
        if (!running) return;
        String command = sequence.commands().isEmpty() ? null : sequence.commands().get(sequence.index());
        if (command == null) return;
        if (waitTicks < 0) waitTicks = rule.nextDelayTicks(rng);
        if (effects.ticksSinceLastSend(tick) < waitTicks) return;
        if (!effects.submit(command, SEND_NOW)) return;
        waitTicks = -1L;
        sequence.next();
        effects.log("COMMAND", command);
    }
}
