package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.config.gui.PresetField;
import dev.exdede.ahtools.gui.ClickGuiScreen;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.gui.row.BaseRow;
import dev.exdede.ahtools.gui.row.LabelRow;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.row.Rows;
import dev.exdede.ahtools.mc.Tr;
import dev.exdede.ahtools.seller.Preset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One expandable row per preset, its fields driven off the PresetField enum so
 * adding a field stays a one line change. Toggles flip in place; everything
 * else edits inline and rejects a bad value by staying open.
 *
 * Every label here is a supplier, because rows are built once per tab switch:
 * baking the current value into the label text is what made a click look like
 * it did nothing until the screen was reopened.
 *
 * Renaming replaces the stored preset under its new name and moves the
 * selection with it, so the preset the player was pointing at stays the one
 * they are pointing at.
 */
public final class PresetsTab {
    private PresetsTab() {}

    public static List<Row> build() {
        List<Row> rows = new ArrayList<>();
        for (Preset preset : AhTools.INSTANCE.presets().presets()) {
            rows.add(presetRow(preset.name()));
        }
        rows.add(Rows.label(""));
        rows.add(Rows.text(Tr.t("ahtools.presets.add"), () -> "", PresetsTab::add)
            .withTooltip(Tr.t("ahtools.tip.preset_add")));
        return rows;
    }

    /**
     * Rows hold the preset name rather than the Preset itself: every edit
     * replaces the record, and a captured copy would go stale on the first
     * change.
     */
    private static Row presetRow(String name) {
        BaseRow row = new LabelRow(
            () -> (selected(name) ? "* " : "  ") + name,
            () -> selected(name) ? Theme.accent() : Theme.TEXT);
        row.withTooltip(Tr.t("ahtools.tip.preset_row"));

        row.withChild(Rows.label(() -> sellingWith(name) ? "  " + Tr.t("ahtools.presets.restart_note") : "",
            () -> Theme.TEXT_DIM));
        row.withChild(Rows.action(() -> "  " + Tr.t(selected(name) ? "ahtools.presets.selected" : "ahtools.presets.select"), () -> {
            AhTools.INSTANCE.presets().select(name);
            AhTools.INSTANCE.presets().save();
        }).withTooltip(Tr.t("ahtools.tip.preset_select")));
        for (PresetField.Kind kind : PresetField.Kind.values()) {
            row.withChild(fieldRow(name, kind));
        }
        row.withChild(Rows.action("  " + Tr.t("ahtools.presets.delete"), () -> {
            AhTools.INSTANCE.presets().remove(name);
            AhTools.INSTANCE.presets().save();
            ClickGuiScreen.markDirty();
        }).withTooltip(Tr.t("ahtools.tip.preset_delete")));
        return row;
    }

    private static Row fieldRow(String name, PresetField.Kind kind) {
        String tip = Tr.t(PresetField.tooltipKey(kind));
        if (PresetField.isToggle(kind)) {
            return Rows.flip("  " + Tr.t(PresetField.labelKey(kind)),
                () -> display(name, kind),
                () -> edit(name, kind, "")).withTooltip(tip);
        }
        return Rows.text("  " + Tr.t(PresetField.labelKey(kind)),
            () -> display(name, kind),
            text -> edit(name, kind, text)).withTooltip(tip);
    }

    private static boolean selected(String name) {
        return name.equals(AhTools.INSTANCE.presets().selectedName());
    }

    /** The running seller holds a copy of its preset from the moment it started. */
    private static boolean sellingWith(String name) {
        AhTools mod = AhTools.INSTANCE;
        return mod.runner().sellerRunning() && mod.seller().preset() != null
            && name.equals(mod.seller().preset().name());
    }

    private static String display(String name, PresetField.Kind kind) {
        Preset preset = AhTools.INSTANCE.presets().find(name);
        if (preset == null) return "";
        // Enum-like values and the empty item are words, so they come from the
        // lang files; prices, delays and names are shown as they are.
        return switch (kind) {
            case EXPECTED_ITEM -> preset.expectedItem().isBlank()
                ? Tr.t("ahtools.preset.any") : preset.expectedItem();
            case PRICING_MODE -> Tr.t("ahtools.pricing." + preset.pricingMode().name().toLowerCase(Locale.ROOT));
            case DELAY_MODE -> Tr.t("ahtools.delay_mode." + preset.delayMode().name().toLowerCase(Locale.ROOT));
            case VALIDATE_HOTBAR -> Tr.t(preset.validateHotbar() ? "ahtools.common.on" : "ahtools.common.off");
            default -> PresetField.display(preset, kind);
        };
    }

    /**
     * @throws IllegalArgumentException when the value is unusable or the new
     * name is taken, which keeps the row in edit mode. Renaming the preset the
     * seller is running is refused too: its sales would keep landing under the
     * old name in the statistics.
     */
    private static void edit(String name, PresetField.Kind kind, String text) {
        Preset preset = AhTools.INSTANCE.presets().find(name);
        if (preset == null) throw new IllegalArgumentException("preset is gone");

        Preset edited = PresetField.apply(preset, kind, text);
        boolean renamed = !edited.name().equals(name);
        if (renamed && sellingWith(name)) throw new IllegalArgumentException("stop the seller to rename");
        if (!AhTools.INSTANCE.presets().rename(name, edited)) throw new IllegalArgumentException("name taken");
        if (renamed) ClickGuiScreen.markDirty();
        AhTools.INSTANCE.presets().save();
    }

    private static void add(String text) {
        String name = text == null ? "" : text.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("a preset needs a name");
        if (!AhTools.INSTANCE.presets().add(Preset.defaults(name))) throw new IllegalArgumentException("name taken");
        AhTools.INSTANCE.presets().save();
        ClickGuiScreen.markDirty();
    }
}
