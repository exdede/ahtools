package dev.exdede.ahtools.core;

import dev.exdede.ahtools.seller.Preset;
import dev.exdede.ahtools.seller.SellCycle;
import dev.exdede.ahtools.spammer.SpamController;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Owns the two automated features and the one gate they share.
 *
 * Both features are toggles rather than start and stop pairs, and both are
 * killed by the same two paths: the emergency stop hotkey and leaving a
 * server. Neither path touches the tracker or the statistics, since money that
 * already arrived is not automation state.
 *
 * Nothing here ever restarts anything. Coming back from a disconnect leaves
 * both toggles off, deliberately: a mod that resumes selling on its own the
 * moment you rejoin is a mod that sells while you are not looking.
 *
 * Starting either feature needs the ban risk warning accepted. The refusal
 * lives here rather than only in the GUI, so a key binding cannot skip the
 * warning. Stopping always works.
 */
public final class AutomationRunner {
    private final ChatSendGate gate;
    private final SellCycle seller;
    private final SpamController spammer;
    private final BooleanSupplier riskAccepted;

    public AutomationRunner(ChatSendGate gate, SellCycle seller, SpamController spammer, BooleanSupplier riskAccepted) {
        this.gate = gate;
        this.seller = seller;
        this.spammer = spammer;
        this.riskAccepted = riskAccepted;
    }

    public boolean riskAccepted() { return riskAccepted.getAsBoolean(); }

    public boolean sellerRunning() { return seller.running(); }
    public boolean spammerRunning() { return spammer.running(); }
    public SellCycle seller() { return seller; }
    public SpamController spammer() { return spammer; }

    /** @return the new running state; false also means a refused start, see SellCycle.lastError(). */
    public boolean toggleSeller(Preset preset, long tick) {
        if (seller.running()) {
            seller.stop();
            clearPendingFrom("seller");
            return false;
        }
        if (preset == null) return false;
        if (!riskAccepted.getAsBoolean()) return false;
        return seller.start(preset, tick);
    }

    /** @return the new running state; false also means a refused start, see SpamController.lastError(). */
    public boolean toggleSpammer(List<String> commands, DelayRule rule, long tick) {
        if (spammer.running()) {
            spammer.stop();
            clearPendingFrom("spammer");
            return false;
        }
        if (!riskAccepted.getAsBoolean()) return false;
        return spammer.start(commands, rule);
    }

    private void clearPendingFrom(String origin) {
        // Only drop the queued message when it belongs to the feature being
        // stopped, so stopping one does not swallow the other's turn.
        if (origin.equals(gate.pendingOrigin())) gate.clear();
    }

    /**
     * Both features submit only once their own delay is over, so when both are
     * ready in the same tick the one ticked first takes the gate. The order
     * flips after every send, which keeps either one from starving the other.
     *
     * A seller that stopped itself on an error mid tick may not leave its
     * command behind in the gate: nothing it sends after stopping is wanted.
     */
    public void tick(long tick) {
        if ("seller".equals(gate.lastSentOrigin())) {
            spammer.tick(tick);
            tickSeller(tick);
        }
        else {
            tickSeller(tick);
            spammer.tick(tick);
        }
        gate.tick(tick);
    }

    private void tickSeller(long tick) {
        seller.tick(tick);
        if (!seller.running()) clearPendingFrom("seller");
    }

    public void onChatLine(String line, long tick) {
        seller.onChatLine(line, tick);
    }

    /** Immediate halt: both features off, the queued message dropped unsent. */
    public void emergencyStop() {
        if (seller.running()) seller.stop();
        if (spammer.running()) spammer.stop();
        gate.clear();
    }

    /** Leaving a server, changing server, returning to the title screen, or quitting. */
    public void onDisconnect() {
        emergencyStop();
    }
}
