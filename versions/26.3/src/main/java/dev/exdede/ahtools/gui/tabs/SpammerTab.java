package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.gui.ClickGuiScreen;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.gui.row.BaseRow;
import dev.exdede.ahtools.gui.row.LabelRow;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.row.Rows;
import dev.exdede.ahtools.gui.row.ToggleRow;
import dev.exdede.ahtools.mc.Tr;

import java.util.ArrayList;
import java.util.List;

/**
 * The add field is prefilled with a slash, since every useful entry here is a
 * command and typing the slash first is the step people forget.
 */
public final class SpammerTab {
    private SpammerTab() {}

    public static List<Row> build() {
        AhTools mod = AhTools.INSTANCE;
        List<Row> rows = new ArrayList<>();

        rows.add(new ToggleRow(Tr.t("ahtools.spammer.toggle"), () -> mod.runner().spammerRunning(), mod::toggleSpammer)
            .withTooltip(Tr.t("ahtools.tip.spammer_toggle")));
        rows.add(Rows.toggle(Tr.t("ahtools.spammer.random_delay"), Configs.General.SPAMMER_RANDOM_DELAY)
            .withTooltip(Tr.t("ahtools.tip.spammer_random_delay")));
        rows.add(Rows.slider(Tr.t("ahtools.spammer.delay"), Configs.General.SPAMMER_DELAY_TICKS)
            .withTooltip(Tr.t("ahtools.tip.spammer_delay")));
        rows.add(Rows.slider(Tr.t("ahtools.spammer.min_delay"), Configs.General.SPAMMER_MIN_DELAY_TICKS)
            .withTooltip(Tr.t("ahtools.tip.spammer_min_delay")));
        rows.add(Rows.slider(Tr.t("ahtools.spammer.max_delay"), Configs.General.SPAMMER_MAX_DELAY_TICKS)
            .withTooltip(Tr.t("ahtools.tip.spammer_max_delay")));
        rows.add(Rows.label(""));

        List<String> commands = Configs.General.SPAM_COMMANDS.get();
        BaseRow list = new LabelRow(
            () -> Tr.t("ahtools.spammer.commands", Configs.General.SPAM_COMMANDS.get().size()),
            () -> Theme.TEXT);
        list.withTooltip(Tr.t("ahtools.tip.spammer_commands"));
        for (String command : commands) {
            list.withChild(Rows.action("  x  " + command, () -> removeCommand(command))
                .withTooltip(Tr.t("ahtools.tip.spammer_remove")));
        }
        rows.add(list);
        rows.add(Rows.text(Tr.t("ahtools.spammer.add"), () -> "/", SpammerTab::addCommand)
            .withTooltip(Tr.t("ahtools.tip.spammer_add")));
        return rows;
    }

    private static void addCommand(String text) {
        if (text == null || text.isBlank() || text.trim().equals("/")) {
            throw new IllegalArgumentException("empty command");
        }
        List<String> commands = new ArrayList<>(Configs.General.SPAM_COMMANDS.get());
        commands.add(text.trim());
        Configs.General.SPAM_COMMANDS.set(commands);
        Configs.saveToFile();
        ClickGuiScreen.markDirty();
    }

    private static void removeCommand(String command) {
        List<String> commands = new ArrayList<>(Configs.General.SPAM_COMMANDS.get());
        commands.remove(command);
        Configs.General.SPAM_COMMANDS.set(commands);
        Configs.saveToFile();
        ClickGuiScreen.markDirty();
    }
}
