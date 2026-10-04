package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.gui.row.BaseRow;
import dev.exdede.ahtools.gui.row.LabelRow;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.row.Rows;
import dev.exdede.ahtools.mc.Tr;
import dev.exdede.ahtools.tracker.Money;
import dev.exdede.ahtools.tracker.StatBucket;
import dev.exdede.ahtools.tracker.StatsStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lifetime totals cannot be rebuilt from anywhere, so that reset asks for the
 * word rather than trusting a single click. The store also writes a
 * timestamped backup before clearing.
 */
public final class StatisticsTab {
    private StatisticsTab() {}

    public static List<Row> build() {
        StatsStore stats = AhTools.INSTANCE.stats();
        List<Row> rows = new ArrayList<>();

        rows.add(Rows.label(Tr.t("ahtools.stats.rounded_hint"), Theme.TEXT_DIM));
        rows.add(bucket(Tr.t("ahtools.stats.session"), () -> stats.session(null))
            .withTooltip(Tr.t("ahtools.tip.stats_session")));
        rows.add(bucket(Tr.t("ahtools.stats.today"), () -> stats.today(null))
            .withTooltip(Tr.t("ahtools.tip.stats_today")));
        rows.add(bucket(Tr.t("ahtools.stats.lifetime"), () -> stats.lifetime(null))
            .withTooltip(Tr.t("ahtools.tip.stats_lifetime")));
        rows.add(Rows.label(""));
        for (String preset : stats.presetNames()) {
            String label = StatsStore.UNATTRIBUTED.equals(preset) ? Tr.t("ahtools.stats.unattributed") : preset;
            rows.add(bucket(label, () -> stats.lifetime(preset)).withTooltip(Tr.t(
                StatsStore.UNATTRIBUTED.equals(preset) ? "ahtools.tip.stats_unattributed" : "ahtools.tip.stats_preset")));
        }
        rows.add(Rows.label(""));
        rows.add(Rows.action(Tr.t("ahtools.stats.reset_session"), AhTools.INSTANCE::resetSession)
            .withTooltip(Tr.t("ahtools.tip.reset_session")));
        rows.add(Rows.action(Tr.t("ahtools.stats.reset_today"), () -> {
            stats.resetToday();
            AhTools.INSTANCE.saveAll();
        }).withTooltip(Tr.t("ahtools.tip.reset_today")));
        rows.add(Rows.text(Tr.t("ahtools.stats.reset_lifetime"), () -> "", text -> {
            if (!"RESET".equals(text == null ? "" : text.trim())) {
                throw new IllegalArgumentException("confirmation did not match");
            }
            stats.resetLifetime();
        }).withTooltip(Tr.t("ahtools.tip.reset_lifetime")));
        return rows;
    }

    /**
     * One expandable row per horizon: profit is what people look at, and the
     * revenue and cost it is made of are one click away. The bucket is fetched
     * per frame rather than passed in, so a sale landing while the tab is open
     * shows up without a tab switch.
     */
    private static BaseRow bucket(String label, Supplier<StatBucket> bucket) {
        BaseRow row = new LabelRow(
            () -> label + ": " + Money.format(bucket.get().profit()),
            () -> bucket.get().profit() < 0 ? Theme.PROFIT_NEGATIVE : Theme.PROFIT_POSITIVE);
        return row
            .withChild(Rows.label(() -> "  " + Tr.t("ahtools.stats.revenue", Money.format(bucket.get().revenue()))))
            .withChild(Rows.label(() -> "  " + Tr.t("ahtools.stats.cost", Money.format(bucket.get().cost()))))
            .withChild(Rows.label(() -> "  " + Tr.t("ahtools.stats.items_sales",
                bucket.get().items(), bucket.get().sales())));
    }
}
