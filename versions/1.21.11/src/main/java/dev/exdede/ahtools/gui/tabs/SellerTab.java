package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.gui.row.CycleRow;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.row.Rows;
import dev.exdede.ahtools.gui.row.ToggleRow;
import dev.exdede.ahtools.mc.Tr;
import dev.exdede.ahtools.seller.Preset;
import dev.exdede.ahtools.tracker.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Live state is drawn as labels because it is read only and changes under the
 * player. Their text is supplied per frame rather than baked in at build time,
 * so the phase and the last slot keep up while the screen stays open; the
 * slot and error lines hold an empty row when they have nothing to say, since
 * a row appearing and vanishing would shift everything under it.
 */
public final class SellerTab {
    private SellerTab() {}

    public static List<Row> build() {
        AhTools mod = AhTools.INSTANCE;
        List<Row> rows = new ArrayList<>();

        rows.add(new ToggleRow(Tr.t("ahtools.seller.toggle"), () -> mod.runner().sellerRunning(), mod::toggleSeller)
            .withTooltip(Tr.t("ahtools.tip.seller_toggle")));
        rows.add(new CycleRow(Tr.t("ahtools.seller.preset"),
            () -> {
                Preset selected = mod.presets().selected();
                return selected == null ? Tr.t("ahtools.seller.none") : selected.name();
            },
            () -> cyclePreset(1), () -> cyclePreset(-1))
            .withTooltip(Tr.t("ahtools.tip.seller_preset")));
        rows.add(Rows.label(() -> {
                Preset selected = mod.presets().selected();
                return selected != null && selected.expectedItem().isBlank()
                    ? Tr.t("ahtools.seller.no_expected_item") : "";
            },
            () -> Theme.PROFIT_NEGATIVE));
        rows.add(Rows.action(Tr.t("ahtools.common.emergency_stop"), mod::emergencyStop)
            .withTooltip(Tr.t("ahtools.tip.emergency_stop")));
        rows.add(Rows.label(""));
        rows.add(Rows.label(() -> Tr.t(
            mod.runner().sellerRunning() ? "ahtools.seller.state_running" : "ahtools.seller.state_stopped",
            Tr.t("ahtools.phase." + mod.seller().phase().name().toLowerCase(Locale.ROOT)))));
        rows.add(Rows.label(() -> mod.seller().currentSlot() < 0 ? ""
            : Tr.t("ahtools.seller.last_slot", mod.seller().currentSlot(),
                Money.format(mod.seller().currentPrice()))));
        rows.add(Rows.label(() -> mod.seller().lastError().isEmpty() ? ""
                : Tr.t("ahtools.seller.last_error", mod.seller().lastError()),
            () -> Theme.PROFIT_NEGATIVE));
        return rows;
    }

    /** Direction is plus or minus one; the list wraps either way. */
    static void cyclePreset(int direction) {
        List<Preset> presets = AhTools.INSTANCE.presets().presets();
        if (presets.isEmpty()) return;
        String current = AhTools.INSTANCE.presets().selectedName();
        int index = 0;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).name().equals(current)) index = i;
        }
        int next = Math.floorMod(index + direction, presets.size());
        AhTools.INSTANCE.presets().select(presets.get(next).name());
        AhTools.INSTANCE.presets().save();
    }
}
