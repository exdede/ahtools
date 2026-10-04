package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.config.BoolSetting;
import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.config.EnumSetting;
import dev.exdede.ahtools.config.IntSetting;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.mc.Tr;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Factories that bind a row to a setting and save the file afterwards, so no
 * tab builder has to remember to persist what it changed.
 */
public final class Rows {
    private Rows() {}

    public static ToggleRow toggle(String label, BoolSetting setting) {
        return new ToggleRow(label, setting::get, () -> {
            setting.toggle();
            Configs.saveToFile();
        });
    }

    public static SliderRow slider(String label, IntSetting setting) {
        return new SliderRow(label, setting, Configs::saveToFile);
    }

    /** Each value is shown as the lang key valueKeyPrefix + the constant's name in lower case. */
    public static <E extends Enum<E>> CycleRow cycle(String label, EnumSetting<E> setting, String valueKeyPrefix) {
        return new CycleRow(label, () -> Tr.t(valueKeyPrefix + setting.get().name().toLowerCase(Locale.ROOT)), () -> {
            setting.cycle();
            Configs.saveToFile();
        }, () -> {
            E[] values = setting.values();
            for (int i = 0; i < values.length - 1; i++) setting.cycle();
            Configs.saveToFile();
        });
    }

    public static LabelRow label(String text) { return new LabelRow(text, Theme.TEXT_MUTED); }

    public static LabelRow label(String text, int color) { return new LabelRow(text, color); }

    /** A label whose text is recomputed every frame, for a value that changes while the screen is open. */
    public static LabelRow label(Supplier<String> text) {
        return new LabelRow(text, () -> Theme.TEXT_MUTED);
    }

    public static LabelRow label(Supplier<String> text, IntSupplier color) {
        return new LabelRow(text, color);
    }

    public static ActionRow action(String label, Runnable action) { return new ActionRow(label, action); }

    /** An action whose text reports the state it changes, so the click shows immediately. */
    public static ActionRow action(Supplier<String> label, Runnable action) {
        return new ActionRow(label, action);
    }

    /**
     * A two state field drawn like a cycle row: the label stays put and the
     * value on the right is read live, so either mouse button flips it in place.
     */
    public static CycleRow flip(String label, Supplier<String> value, Runnable toggle) {
        return new CycleRow(label, value, toggle, toggle);
    }

    public static TextRow text(String label, Supplier<String> current, Consumer<String> onCommit) {
        return new TextRow(label, current, onCommit);
    }
}
