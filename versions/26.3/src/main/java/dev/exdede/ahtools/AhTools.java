package dev.exdede.ahtools;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.core.AutomationRunner;
import dev.exdede.ahtools.core.ChatSendGate;
import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.core.RiskConsent;
import dev.exdede.ahtools.hud.ProfitHud;
import dev.exdede.ahtools.mc.ChatOut;
import dev.exdede.ahtools.mc.Feedback;
import dev.exdede.ahtools.mc.LiveSellEffects;
import dev.exdede.ahtools.mc.Screens;
import dev.exdede.ahtools.mc.Tr;
import dev.exdede.ahtools.mc.TrLabels;
import dev.exdede.ahtools.seller.Preset;
import dev.exdede.ahtools.seller.PresetStore;
import dev.exdede.ahtools.seller.SellCycle;
import dev.exdede.ahtools.spammer.SpamController;
import dev.exdede.ahtools.spammer.SpamEffects;
import dev.exdede.ahtools.tracker.ListingLedger;
import dev.exdede.ahtools.tracker.SaleTracker;
import dev.exdede.ahtools.tracker.StatsStore;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;

/**
 * Constructed once at startup and held statically: no dependency injection,
 * just one place that knows how the pieces fit together.
 *
 * Persistence is written on a debounce rather than on every sale, so a busy
 * market does not turn into a disk write per chat line, and flushed on
 * disconnect so a crash loses at most the debounce window.
 */
public final class AhTools {
    /** How long after the last change to write statistics and the ledger to disk. */
    private static final long SAVE_DEBOUNCE_TICKS = 100L;

    public static AhTools INSTANCE;

    private final PresetStore presets;
    private final StatsStore stats;
    private final ListingLedger ledger;
    private final SaleTracker tracker;
    private final ChatSendGate gate;
    private final AutomationRunner runner;
    private final SellCycle seller;
    private final SpamController spammer;

    private long tick;
    private boolean sellerWasRunning;
    private long dirtySinceTick = -1L;
    private long sessionStartMillis = System.currentTimeMillis();

    private final ProfitHud hud = new ProfitHud(new TrLabels());

    public AhTools(Path dataDir) {
        this.presets = new PresetStore(dataDir.resolve("presets.json"));
        this.stats = new StatsStore(dataDir.resolve("stats.json"), () -> LocalDate.now().toString());
        this.ledger = new ListingLedger(dataDir.resolve("ledger.json"));
        this.gate = new ChatSendGate(ChatOut::send, new Random());

        this.seller = new SellCycle(new LiveSellEffects(gate), new Random());
        SpamEffects spamEffects = new SpamEffects() {
            @Override public long ticksSinceLastSend(long tick) {
                return gate.ticksSinceLastSend(tick);
            }
            @Override public boolean submit(String command, DelayRule rule) {
                return gate.submit(command, "spammer", rule);
            }
            @Override public void log(String kind, String message) {
                if (Configs.General.DEBUG_LOGGING.get()) {
                    AhToolsMod.LOGGER.info("[{}] {}", kind, message);
                }
            }
        };
        this.spammer = new SpamController(spamEffects);
        this.runner = new AutomationRunner(gate, seller, spammer,
            () -> RiskConsent.accepted(Configs.General.RISK_ACCEPTED_VERSION.get()));

        this.tracker = new SaleTracker(ledger, stats, new SaleTracker.Context() {
            @Override public String activePresetName() {
                return seller.running() && seller.preset() != null ? seller.preset().name() : null;
            }
            @Override public long costPerItem() {
                return seller.preset() == null ? 0L : seller.preset().costPerItem();
            }
        });

        presets.load();
        stats.load();
        ledger.load();
        ledger.pruneExpired(System.currentTimeMillis());
    }

    public PresetStore presets() { return presets; }
    public StatsStore stats() { return stats; }
    public ListingLedger ledger() { return ledger; }
    public SaleTracker tracker() { return tracker; }
    public ChatSendGate gate() { return gate; }
    public AutomationRunner runner() { return runner; }
    public SellCycle seller() { return seller; }
    public SpamController spammer() { return spammer; }
    public long tick() { return tick; }
    public long sessionStartMillis() { return sessionStartMillis; }
    public ProfitHud hud() { return hud; }

    public void onClientTick() {
        tick++;
        runner.tick(tick);
        // The seller can stop itself on an error at any moment, usually while
        // the player is not looking at the screen that would show it.
        if (sellerWasRunning && !seller.running() && seller.phase() == SellCycle.Phase.STOPPED) {
            Feedback.alert(Tr.t("ahtools.msg.seller_stopped_error", seller.lastError()));
        }
        sellerWasRunning = seller.running();
        saveIfDue();
    }

    public void onChatLine(String line) {
        runner.onChatLine(line, tick);

        // Diffing the session bucket is what tells the HUD a sale landed:
        // the tracker reports every money line, including listings that move
        // no money, and only the bucket knows which of them actually paid.
        long before = stats.session(null).profit();
        if (tracker.onChatLine(line, System.currentTimeMillis()) != null) {
            markDirty();
            long delta = stats.session(null).profit() - before;
            if (delta != 0L) hud.onSale(delta, tick);
        }
    }

    public void toggleSeller() {
        // Accepting stores the consent before running this again, so it cannot loop.
        if (!runner.sellerRunning() && !runner.riskAccepted()) {
            Screens.openRiskWarning(this::toggleSeller);
            return;
        }
        Preset preset = presets.selected();
        if (preset == null) {
            Feedback.message(Tr.t("ahtools.msg.no_preset"));
            return;
        }
        boolean running = runner.toggleSeller(preset, tick);
        if (running) {
            Feedback.message(Tr.t("ahtools.msg.seller_started", preset.name()));
        }
        else if (!seller.lastError().isEmpty()) {
            Feedback.message(Tr.t("ahtools.msg.seller_refused", seller.lastError()));
        }
        else {
            Feedback.message(Tr.t("ahtools.msg.seller_stopped"));
        }
    }

    public void toggleSpammer() {
        if (!runner.spammerRunning() && !runner.riskAccepted()) {
            Screens.openRiskWarning(this::toggleSpammer);
            return;
        }
        boolean running = runner.toggleSpammer(
            Configs.General.SPAM_COMMANDS.get(), Configs.spammerDelayRule(), tick);
        Feedback.message(running
            ? Tr.t("ahtools.msg.spammer_started")
            : (spammer.lastError().isEmpty()
                ? Tr.t("ahtools.msg.spammer_stopped")
                : Tr.t("ahtools.msg.spammer_error", spammer.lastError())));
    }

    public void emergencyStop() {
        runner.emergencyStop();
        Feedback.message(Tr.t("ahtools.msg.emergency_stop"));
    }

    public void onDisconnect() {
        runner.onDisconnect();
        sellerWasRunning = false;
        saveAll();
    }

    /**
     * A new world (a server switch inside the network, the limbo during a
     * restart, a dimension change) ends automation the same way leaving does.
     * /ah may not exist where the player landed, and nothing should keep
     * sending into a place the player did not choose.
     */
    public void onWorldChange() {
        boolean wasRunning = runner.sellerRunning() || runner.spammerRunning();
        runner.emergencyStop();
        sellerWasRunning = false;
        if (wasRunning) Feedback.message(Tr.t("ahtools.msg.world_changed"));
    }

    public void markDirty() {
        if (dirtySinceTick < 0) dirtySinceTick = tick;
    }

    private void saveIfDue() {
        if (dirtySinceTick < 0 || tick - dirtySinceTick < SAVE_DEBOUNCE_TICKS) return;
        saveAll();
    }

    /**
     * Resets the session bucket and the clock the HUD's rate is measured
     * against together, so the rate always matches the number beside it.
     */
    public void resetSession() {
        stats.resetSession();
        sessionStartMillis = System.currentTimeMillis();
    }

    public void saveAll() {
        dirtySinceTick = -1L;
        stats.save();
        ledger.save();
        presets.save();
    }
}
